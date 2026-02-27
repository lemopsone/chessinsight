#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import math
import os
import subprocess
import sys
import time
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parents[2]
AUTH_STATE_FILE = ROOT_DIR / "reports" / "perf" / "manual-state" / "auth.json"
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
    return parser.parse_args()


def fail(msg: str) -> None:
    print(f"ERROR: {msg}", file=sys.stderr)
    raise SystemExit(1)


def load_token() -> str:
    if not AUTH_STATE_FILE.is_file():
        fail(f"auth file not found: {AUTH_STATE_FILE}. Run ci/perf/manual_prepare_analysis_state.py once.")
    try:
        state = json.loads(AUTH_STATE_FILE.read_text(encoding="utf-8"))
    except Exception as exc:  # noqa: BLE001
        fail(f"cannot parse auth file: {exc}")
    token = str(state.get("token") or state.get("accessToken") or "").strip()
    if not token:
        fail(f"token is missing in auth file: {AUTH_STATE_FILE}")
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


def run_k6(mode: str, rps: int, max_rps: int, token: str) -> int:
    compose_project = os.getenv("COMPOSE_PROJECT_NAME", "chessinsight")
    network_name = f"{compose_project}_chessnet"
    load_reference = rps if mode == "fixed" else max_rps
    pre_allocated_vus = max(4, int(math.ceil(load_reference * 4.0)))
    max_vus = max(64, int(math.ceil(load_reference * 40.0)), pre_allocated_vus)
    container_name = f"manual-k6-move-analysis-{int(time.time())}"

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
        f"BASE_URL={BASE_URL}",
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
            f"DURATION={DURATION}",
            "-e",
            f"PHASE={PHASE}-fixed",
        ])
    else:
        cmd.extend([
            "-e",
            f"MAX_RPS={max_rps}",
            "-e",
            f"RAMP_STEP_DURATION={RAMP_STEP_DURATION}",
            "-e",
            f"RAMP_HOLD_DURATION={RAMP_HOLD_DURATION}",
            "-e",
            f"PHASE={PHASE}-ramping",
        ])

    # Keep k6 command at the end after env vars are applied.
    cmd.extend([K6_IMAGE, "run", K6_SCRIPT_PATH])

    print(
        f"[k6] mode={mode} rps={rps if mode == 'fixed' else max_rps} "
        f"network={network_name} container={container_name} "
        f"(Ctrl+C to stop)",
        file=sys.stderr,
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
    return run_k6(args.mode, args.rps, args.max_rps, token)


if __name__ == "__main__":
    raise SystemExit(main())
