#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import math
import os
import subprocess
import sys
import time
import uuid
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parents[2]
AUTH_STATE_ENV = "PERF_AUTH_STATE_FILE"
DEFAULT_AUTH_STATE_FILE = Path("/tmp/chessinsight-perf/auth.json")
LEGACY_AUTH_STATE_FILE = ROOT_DIR / "reports" / "perf" / "manual-state" / "auth.json"
MOVE_PAYLOADS_FILE = ROOT_DIR / "ci" / "perf" / "move-analysis-payloads.json"
K6_SCRIPT_DIR = ROOT_DIR / "ci" / "perf"
K6_SCRIPT_PATH = "/scripts/k6-analysis-rps.js"
K6_IMAGE = "grafana/k6:0.56.0"
BASE_URL = "http://backend:8080"
PHASE = "move-analysis"
DURATION = "24h"
RAMP_STEP_DURATION = "30s"
RAMP_HOLD_DURATION = "60s"
HTTP_TIMEOUT = "60s"
GRACEFUL_STOP = "5s"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run manual k6 load for /move-evaluations")
    parser.add_argument("--mode", choices=["fixed", "ramping"], default="fixed")
    parser.add_argument("--rps", type=int, default=0, help="Fixed mode target RPS")
    parser.add_argument("--max-rps", type=int, default=0, help="Ramping mode max RPS")
    parser.add_argument("--duration", default=DURATION, help="Fixed mode duration, e.g. 45s, 2m")
    parser.add_argument("--ramp-step-duration", default=RAMP_STEP_DURATION, help="Ramping step duration")
    parser.add_argument("--ramp-hold-duration", default=RAMP_HOLD_DURATION, help="Ramping hold duration")
    parser.add_argument("--base-url", default=BASE_URL, help="Target base URL from k6 container network")
    parser.add_argument("--phase", default=PHASE, help="k6 scenario phase/tag label")
    return parser.parse_args()


def fail(msg: str) -> None:
    print(f"ERROR: {msg}", file=sys.stderr)
    raise SystemExit(1)


def auth_state_candidates() -> list[Path]:
    env_path = os.getenv(AUTH_STATE_ENV, "").strip()
    if env_path:
        return [Path(env_path), LEGACY_AUTH_STATE_FILE, DEFAULT_AUTH_STATE_FILE]
    return [DEFAULT_AUTH_STATE_FILE, LEGACY_AUTH_STATE_FILE]


def load_token() -> str:
    auth_file = next((candidate for candidate in auth_state_candidates() if candidate.is_file()), None)
    if auth_file is None:
        checked = ", ".join(str(candidate) for candidate in auth_state_candidates())
        fail(f"auth file not found (checked: {checked}). Run ci/perf/manual_prepare_analysis_state.py once.")
    try:
        state = json.loads(auth_file.read_text(encoding="utf-8"))
    except Exception as exc:  # noqa: BLE001
        fail(f"cannot parse auth file: {exc}")
    token = str(state.get("token") or state.get("accessToken") or "").strip()
    if not token:
        fail(f"token is missing in auth file: {auth_file}")
    return token


def validate_payload() -> None:
    if not MOVE_PAYLOADS_FILE.is_file():
        fail(f"payload file not found: {MOVE_PAYLOADS_FILE}")
    try:
        payload_source = json.loads(MOVE_PAYLOADS_FILE.read_text(encoding="utf-8"))
    except Exception as exc:  # noqa: BLE001
        fail(f"cannot parse payload file: {exc}")

    payload = payload_source
    if isinstance(payload_source, list):
        if len(payload_source) != 1:
            fail("payload file must contain exactly one payload")
        payload = payload_source[0]
    if not isinstance(payload, dict):
        fail("payload must be a JSON object")
    for field in ("moveNum", "positionFEN", "moveSAN", "moveUCI"):
        if field not in payload:
            fail(f"payload missing required field: {field}")


def run_k6(
    mode: str,
    rps: int,
    max_rps: int,
    token: str,
    duration: str,
    ramp_step_duration: str,
    ramp_hold_duration: str,
    base_url: str,
    phase: str,
) -> int:
    compose_project = os.getenv("COMPOSE_PROJECT_NAME", "chessinsight")
    network_name = f"{compose_project}_chessnet"
    load_reference = rps if mode == "fixed" else max_rps
    pre_allocated_vus = max(4, int(math.ceil(load_reference * 4.0)))
    max_vus = max(64, int(math.ceil(load_reference * 40.0)), pre_allocated_vus)
    unique_suffix = uuid.uuid4().hex[:10]
    container_name = f"manual-k6-move-analysis-{int(time.time())}-{unique_suffix}"

    cmd = [
        "docker",
        "run",
        "--rm",
        "--name",
        container_name,
        "--network",
        network_name,
        "-v",
        f"{K6_SCRIPT_DIR}:/scripts:ro",
        "-e",
        f"BASE_URL={base_url}",
        "-e",
        f"TOKEN={token}",
        "-e",
        f"MOVE_PAYLOADS_FILE=/scripts/{MOVE_PAYLOADS_FILE.name}",
        "-e",
        f"MODE={mode}",
        "-e",
        f"HTTP_TIMEOUT={HTTP_TIMEOUT}",
        "-e",
        f"PRE_ALLOCATED_VUS={pre_allocated_vus}",
        "-e",
        f"MAX_VUS={max_vus}",
        "-e",
        f"GRACEFUL_STOP={GRACEFUL_STOP}",
    ]

    if mode == "fixed":
        cmd.extend([
            "-e",
            f"RPS={rps}",
            "-e",
            f"DURATION={duration}",
            "-e",
            f"PHASE={phase}-fixed",
        ])
    else:
        cmd.extend([
            "-e",
            f"MAX_RPS={max_rps}",
            "-e",
            f"RAMP_STEP_DURATION={ramp_step_duration}",
            "-e",
            f"RAMP_HOLD_DURATION={ramp_hold_duration}",
            "-e",
            f"PHASE={phase}-ramping",
        ])

    # Keep k6 command at the end after env vars are applied.
    cmd.extend([K6_IMAGE, "run", K6_SCRIPT_PATH])

    print(
        f"[k6] mode={mode} rps={rps if mode == 'fixed' else max_rps} "
        f"duration={duration if mode == 'fixed' else ramp_step_duration + '+' + ramp_hold_duration} "
        f"network={network_name} container={container_name} "
        f"(Ctrl+C to stop)",
        file=sys.stderr,
    )
    subprocess.run(
        ["docker", "rm", "-f", container_name],
        cwd=ROOT_DIR,
        check=False,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    proc = subprocess.Popen(cmd, cwd=ROOT_DIR)
    try:
        return proc.wait()
    except KeyboardInterrupt:
        print("\n[k6] stopping container...", file=sys.stderr)
        subprocess.run(["docker", "stop", "-t", "2", container_name], cwd=ROOT_DIR, check=False)
        try:
            return proc.wait(timeout=10)
        except subprocess.TimeoutExpired:
            proc.kill()
            return 130


def main() -> int:
    args = parse_args()
    if args.mode == "fixed" and args.rps <= 0:
        fail(f"--rps must be > 0 in fixed mode, got {args.rps}")
    if args.mode == "ramping" and args.max_rps <= 0:
        fail(f"--max-rps must be > 0 in ramping mode, got {args.max_rps}")
    validate_payload()
    token = load_token()
    return run_k6(
        args.mode,
        args.rps,
        args.max_rps,
        token,
        args.duration,
        args.ramp_step_duration,
        args.ramp_hold_duration,
        args.base_url,
        args.phase,
    )


if __name__ == "__main__":
    raise SystemExit(main())
