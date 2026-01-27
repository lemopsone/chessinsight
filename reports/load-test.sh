#!/usr/bin/env bash

# коуртези оф чатгпт ол райтс ресервед
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost}"
LOGIN_OR_EMAIL="${LOGIN_OR_EMAIL:-foo}"
PASSWORD="${PASSWORD:-barbarbar}"
EMAIL="${EMAIL:-test@example.com}"
AUTH_TYPE="${AUTH_TYPE:-JWT}"
SIGNUP="${SIGNUP:-0}"

THREADS="${THREADS:-8}"
CONNS="${CONNS:-200}"
DURATION="${DURATION:-30s}"

OUT="${OUT:-reports/load-test-$(date +%Y%m%d-%H%M%S).md}"

json_escape() {
  python3 - <<'PY'
import json,sys
print(json.dumps(sys.stdin.read()))
PY
}

json_get() {
  python3 - "$1" <<'PY'
import json,sys
try:
    obj=json.loads(sys.argv[1])
    print(obj.get('accessToken',''))
except Exception:
    print("")
PY
}

if [ "$SIGNUP" = "1" ]; then
  curl -sS -X POST "$BASE_URL/api/v1/users" \
    -H "Content-Type: application/json" \
    -d "{\"login\":\"$LOGIN_OR_EMAIL\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" \
    >/dev/null || true
fi

AUTH_RESP=$(curl -sS -X POST "$BASE_URL/api/v1/auth/sessions" \
  -H "Content-Type: application/json" \
  -d "{\"loginOrEmail\":\"$LOGIN_OR_EMAIL\",\"passwordOrToken\":\"$PASSWORD\",\"authType\":\"$AUTH_TYPE\"}" \
  -w "\n%{http_code}")

TOKEN_JSON=$(printf '%s' "$AUTH_RESP" | sed '$d')
AUTH_CODE=$(printf '%s' "$AUTH_RESP" | tail -n 1)

if [ "$AUTH_CODE" != "200" ]; then
  echo "Auth failed: HTTP $AUTH_CODE" >&2
  echo "Response: $TOKEN_JSON" >&2
  exit 1
fi

ACCESS_TOKEN=$(json_get "$TOKEN_JSON")
if [ -z "$ACCESS_TOKEN" ]; then
  echo "Failed to получить accessToken. Ответ: $TOKEN_JSON" >&2
  exit 1
fi

mkdir -p "$(dirname "$OUT")"
{
  echo "# Load Test Report"
  echo
  echo "- Base URL: $BASE_URL"
  echo "- User: $LOGIN_OR_EMAIL"
  echo "- Threads: $THREADS"
  echo "- Connections: $CONNS"
  echo "- Duration: $DURATION"
  echo
} > "$OUT"

run_wrk() {
  local title="$1"
  local script="$2"
  local url="$3"
  echo "## $title" >> "$OUT"
  echo '```' >> "$OUT"
  wrk -t"$THREADS" -c"$CONNS" -d"$DURATION" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -s "$script" \
    "$url" | tee -a "$OUT"
  echo '```' >> "$OUT"
  echo >> "$OUT"
}

run_wrk "GET /api/v1/users/me/training-scenarios?completed=true" "reports/wrk-get.lua" "$BASE_URL/api/v1/users/me/training-scenarios?completed=true"
run_wrk "POST /api/v1/users/me/games (invalid body)" "reports/wrk-post-bad.lua" "$BASE_URL/api/v1/users/me/games"

echo "Report saved to $OUT"
