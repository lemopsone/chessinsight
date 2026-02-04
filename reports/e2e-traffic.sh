#!/usr/bin/env bash

set -euo pipefail

BASE_URL=${BASE_URL:-http://localhost:8080/v1}
OUT_DIR=${OUT_DIR:-reports/traffic}
PORT=${PORT:-8080}

mkdir -p "$OUT_DIR"
PCAP="$OUT_DIR/e2e-traffic.pcap"

sudo tcpdump -i any -w "$PCAP" tcp port "$PORT" &
TCPDUMP_PID=$!

cleanup() {
  sudo kill "$TCPDUMP_PID" 2>/dev/null || true
}
trap cleanup EXIT

sleep 1

LOGIN="user_$(cat /proc/sys/kernel/random/uuid)"
export LOGIN

SIGNUP_PAYLOAD=$(python3 - <<'PY'
import json, os
login = os.environ["LOGIN"]
print(json.dumps({"login": login, "email": login + "@example.com", "password": "pass12345"}))
PY
)

SIGNUP_RESP=$(curl -s -X POST "$BASE_URL/users" -H "Content-Type: application/json" -d "$SIGNUP_PAYLOAD")
ACCESS_TOKEN=$(python3 - <<'PY'
import json, sys
print(json.load(sys.stdin)["accessToken"])
PY
<<< "$SIGNUP_RESP")

PGN_PAYLOAD=$(python3 - <<'PY'
import json
pgn = '[Event "Casual"]\\n[Site "Here"]\\n[Date "2024.01.01"]\\n[Round "1"]\\n[White "W"]\\n[Black "B"]\\n[Result "*"]\\n\\n1. e4 e5 *'
print(json.dumps({"pgn": pgn}))
PY
)

GAME_RESP=$(curl -s -X POST "$BASE_URL/games" -H "Authorization: Bearer $ACCESS_TOKEN" -H "Content-Type: application/json" -d "$PGN_PAYLOAD")
GAME_ID=$(python3 - <<'PY'
import json, sys
print(json.load(sys.stdin)["id"])
PY
<<< "$GAME_RESP")

curl -s -H "Authorization: Bearer $ACCESS_TOKEN" "$BASE_URL/users/me/games" > /dev/null
curl -s -H "Authorization: Bearer $ACCESS_TOKEN" "$BASE_URL/games/$GAME_ID" > /dev/null
curl -s -X DELETE -H "Authorization: Bearer $ACCESS_TOKEN" "$BASE_URL/games/$GAME_ID" > /dev/null
