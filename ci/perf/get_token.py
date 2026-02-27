#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import sys
import time
from pathlib import Path

import requests

ROOT_DIR = Path(__file__).resolve().parents[2]
DEFAULT_AUTH_STATE = ROOT_DIR / "reports" / "perf" / "manual-state" / "auth.json"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Prepare a single shared auth token for manual move-analysis perf tests")
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--login", default="perf_move_user")
    parser.add_argument("--email", default="")
    parser.add_argument("--password", default="perfPass12345")
    parser.add_argument("--request-timeout-sec", type=float, default=30.0)
    parser.add_argument("--out-file", default=str(DEFAULT_AUTH_STATE))
    return parser.parse_args()


def fail(msg: str) -> None:
    print(f"ERROR: {msg}", file=sys.stderr)
    raise SystemExit(1)


def sign_up(base_url: str, login: str, email: str, password: str, timeout_sec: float) -> requests.Response:
    payload = {
        "login": login,
        "email": email,
        "password": password,
    }
    return requests.post(f"{base_url}/api/v1/users", json=payload, timeout=timeout_sec)


def sign_in(base_url: str, login: str, password: str, timeout_sec: float) -> requests.Response:
    payload = {
        "loginOrEmail": login,
        "passwordOrToken": password,
        "authType": "JWT",
    }
    return requests.post(f"{base_url}/api/v1/auth/sessions", json=payload, timeout=timeout_sec)


def main() -> int:
    args = parse_args()
    if len(args.login.strip()) < 3:
        fail("--login must have at least 3 characters")
    if len(args.password) < 8:
        fail("--password must have at least 8 characters")

    base_url = args.base_url.rstrip("/")
    email = args.email.strip() or f"{args.login.strip()}@example.com"

    sign_up_resp = sign_up(base_url, args.login.strip(), email, args.password, args.request_timeout_sec)
    token_response_json: dict[str, str]
    if sign_up_resp.status_code == 201:
        token_response_json = sign_up_resp.json() or {}
        print(f"[auth] user created: {args.login}", file=sys.stderr)
    elif sign_up_resp.status_code == 409:
        sign_in_resp = sign_in(base_url, args.login.strip(), args.password, args.request_timeout_sec)
        if sign_in_resp.status_code != 200:
            fail(f"sign-in failed: status={sign_in_resp.status_code}, body={sign_in_resp.text}")
        token_response_json = sign_in_resp.json() or {}
        print(f"[auth] user exists, signed in: {args.login}", file=sys.stderr)
    else:
        fail(f"sign-up failed: status={sign_up_resp.status_code}, body={sign_up_resp.text}")

    access_token = str(token_response_json.get("accessToken") or "").strip()
    if not access_token:
        fail(f"auth response has no accessToken: {token_response_json}")

    out_file = Path(args.out_file)
    out_file.parent.mkdir(parents=True, exist_ok=True)
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
