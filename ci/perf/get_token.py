#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parents[2]
AUTH_STATE_ENV = "PERF_AUTH_STATE_FILE"
DEFAULT_AUTH_STATE = Path("/tmp/chessinsight-perf/auth.json")
LEGACY_AUTH_STATE = ROOT_DIR / "reports" / "perf" / "manual-state" / "auth.json"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Prepare a single shared auth token for manual move-analysis perf tests")
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--login", default="perf_move_user")
    parser.add_argument("--email", default="")
    parser.add_argument("--password", default="perfPass12345")
    parser.add_argument("--request-timeout-sec", type=float, default=30.0)
    parser.add_argument("--out-file", default=os.getenv(AUTH_STATE_ENV, str(DEFAULT_AUTH_STATE)))
    return parser.parse_args()


def fail(msg: str) -> None:
    print(f"ERROR: {msg}", file=sys.stderr)
    raise SystemExit(1)


def post_json(url: str, payload: dict[str, str], timeout_sec: float) -> tuple[int, dict[str, object], str]:
    req = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json", "Accept": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=timeout_sec) as resp:
            body = resp.read().decode("utf-8")
            status = int(resp.status)
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8")
        status = int(exc.code)
    except urllib.error.URLError as exc:
        fail(f"request failed for {url}: {exc}")

    payload_json: dict[str, object] = {}
    if body.strip():
        try:
            parsed = json.loads(body)
            if isinstance(parsed, dict):
                payload_json = parsed
        except json.JSONDecodeError:
            pass
    return status, payload_json, body


def sign_up(base_url: str, login: str, email: str, password: str, timeout_sec: float) -> tuple[int, dict[str, object], str]:
    payload = {
        "login": login,
        "email": email,
        "password": password,
    }
    return post_json(f"{base_url}/api/v1/users", payload, timeout_sec)


def sign_in(base_url: str, login: str, password: str, timeout_sec: float) -> tuple[int, dict[str, object], str]:
    payload = {
        "loginOrEmail": login,
        "passwordOrToken": password,
        "authType": "JWT",
    }
    return post_json(f"{base_url}/api/v1/auth/sessions", payload, timeout_sec)


def main() -> int:
    args = parse_args()
    if len(args.login.strip()) < 3:
        fail("--login must have at least 3 characters")
    if len(args.password) < 8:
        fail("--password must have at least 8 characters")

    base_url = args.base_url.rstrip("/")
    email = args.email.strip() or f"{args.login.strip()}@example.com"

    sign_up_status, sign_up_json, sign_up_body = sign_up(
        base_url,
        args.login.strip(),
        email,
        args.password,
        args.request_timeout_sec,
    )

    token_response_json: dict[str, object]
    if sign_up_status == 201:
        token_response_json = sign_up_json
        print(f"[auth] user created: {args.login}", file=sys.stderr)
    elif sign_up_status == 409:
        sign_in_status, sign_in_json, sign_in_body = sign_in(
            base_url,
            args.login.strip(),
            args.password,
            args.request_timeout_sec,
        )
        if sign_in_status != 200:
            fail(f"sign-in failed: status={sign_in_status}, body={sign_in_body}")
        token_response_json = sign_in_json
        print(f"[auth] user exists, signed in: {args.login}", file=sys.stderr)
    else:
        fail(f"sign-up failed: status={sign_up_status}, body={sign_up_body}")

    access_token = str(token_response_json.get("accessToken") or "").strip()
    if not access_token:
        fail(f"auth response has no accessToken: {token_response_json}")

    out_file = Path(args.out_file)
    try:
        out_file.parent.mkdir(parents=True, exist_ok=True)
    except PermissionError:
        if out_file == LEGACY_AUTH_STATE:
            out_file = DEFAULT_AUTH_STATE
            out_file.parent.mkdir(parents=True, exist_ok=True)
            print(
                f"[auth] cannot write legacy auth state, fallback to: {out_file}",
                file=sys.stderr,
            )
        else:
            fail(f"cannot create output directory for auth state: {out_file.parent}")
    state = {
        "created_at": time.strftime("%Y-%m-%d %H:%M:%S"),
        "base_url": base_url,
        "login": args.login.strip(),
        "token": access_token,
        "refreshToken": token_response_json.get("refreshToken"),
        "tokenType": token_response_json.get("tokenType"),
    }
    out_file.write_text(json.dumps(state, ensure_ascii=False, indent=2), encoding="utf-8")

    print(out_file.resolve())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
