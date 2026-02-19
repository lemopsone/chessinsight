#!/usr/bin/env bash

set -euo pipefail

RESULTS_DIR=${1:-/reports}
OUT_RESULTS=${2:-$RESULTS_DIR/allure-results-merged}
OUT_REPORT=${3:-$RESULTS_DIR/allure-report}
HISTORY_DIR=${4:-$RESULTS_DIR/allure-history}

rm -rf "$OUT_RESULTS" "$OUT_REPORT"
mkdir -p "$OUT_RESULTS"

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required"
  exit 1
fi

stage_title() {
  case "$1" in
    unit) echo "Unit" ;;
    integration) echo "Integration" ;;
    e2e) echo "E2E" ;;
    *) echo "$1" ;;
  esac
}

write_executor() {
  local repo owner name default_report_url report_url build_url build_order build_name

  repo=${GITHUB_REPOSITORY:-}
  owner=${GITHUB_REPOSITORY_OWNER:-}
  name=${repo##*/}
  if [ -z "$owner" ] && [ -n "$repo" ]; then
    owner=${repo%%/*}
  fi

  default_report_url=""
  if [ -n "$owner" ] && [ -n "$name" ]; then
    if [ "$name" = "${owner}.github.io" ]; then
      default_report_url="https://${owner}.github.io/"
    else
      default_report_url="https://${owner}.github.io/${name}/"
    fi
  fi

  report_url=${REPORT_URL:-$default_report_url}
  build_url=${BUILD_URL:-}
  if [ -z "$build_url" ] && [ -n "${GITHUB_SERVER_URL:-}" ] && [ -n "$repo" ] && [ -n "${GITHUB_RUN_ID:-}" ]; then
    build_url="${GITHUB_SERVER_URL}/${repo}/actions/runs/${GITHUB_RUN_ID}"
  fi

  build_order=${BUILD_ORDER:-${GITHUB_RUN_NUMBER:-0}}
  if ! [[ "$build_order" =~ ^[0-9]+$ ]]; then
    build_order=0
  fi
  build_name=${BUILD_NAME:-"GitHub Actions #${build_order}"}

  if [ -z "$report_url" ] && [ -z "$build_url" ]; then
    return
  fi

  jq -n \
    --arg name "GitHub Actions" \
    --arg type "github" \
    --arg url "${GITHUB_SERVER_URL:-}" \
    --arg buildName "$build_name" \
    --arg buildUrl "$build_url" \
    --arg reportName "Allure Report" \
    --arg reportUrl "$report_url" \
    --argjson buildOrder "$build_order" \
    '{
      name: $name,
      type: $type,
      url: $url,
      buildOrder: $buildOrder,
      buildName: $buildName,
      buildUrl: $buildUrl,
      reportName: $reportName,
      reportUrl: $reportUrl
    }' > "$OUT_RESULTS/executor.json"
}

if [ -d "$HISTORY_DIR" ]; then
  mkdir -p "$OUT_RESULTS/history"
  cp -a "$HISTORY_DIR/." "$OUT_RESULTS/history"
fi

write_executor

for stage in unit integration e2e; do
  STAGE_DIR="$RESULTS_DIR/allure-results-$stage"
  TITLE="$(stage_title "$stage")"
  if [ -d "$STAGE_DIR" ]; then
    cp -a "$STAGE_DIR/." "$OUT_RESULTS"
  else
    TS=$(date +%s%3N)
    UUID_VAL=$(cat /proc/sys/kernel/random/uuid)
    cat > "$OUT_RESULTS/${stage}-skipped-${UUID_VAL}-result.json" <<EOF
{"uuid":"$UUID_VAL","name":"$stage stage skipped","status":"skipped","stage":"finished","start":$TS,"stop":$TS,"labels":[{"name":"parentSuite","value":"$TITLE"},{"name":"epic","value":"Test Stages"},{"name":"feature","value":"$TITLE"},{"name":"tag","value":"$stage"}]}
EOF
  fi
done

allure generate "$OUT_RESULTS" -o "$OUT_REPORT" --clean

rm -rf "$HISTORY_DIR"
mkdir -p "$HISTORY_DIR"
if [ -d "$OUT_REPORT/history" ]; then
  cp -a "$OUT_REPORT/history/." "$HISTORY_DIR"
fi
