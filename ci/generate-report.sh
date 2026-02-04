#!/usr/bin/env bash

set -euo pipefail

RESULTS_DIR=${1:-/reports}
OUT_RESULTS=${2:-$RESULTS_DIR/allure-results-merged}
OUT_REPORT=${3:-$RESULTS_DIR/allure-report}
HISTORY_DIR=${4:-$RESULTS_DIR/allure-history}

rm -rf "$OUT_RESULTS" "$OUT_REPORT"
mkdir -p "$OUT_RESULTS"

if [ -d "$HISTORY_DIR" ]; then
  mkdir -p "$OUT_RESULTS/history"
  cp -a "$HISTORY_DIR/." "$OUT_RESULTS/history"
fi

for stage in unit integration e2e; do
  STAGE_DIR="$RESULTS_DIR/allure-results-$stage"
  if [ -d "$STAGE_DIR" ]; then
    cp -a "$STAGE_DIR/." "$OUT_RESULTS"
  else
    TS=$(date +%s%3N)
    UUID_VAL=$(cat /proc/sys/kernel/random/uuid)
    cat > "$OUT_RESULTS/${stage}-skipped-${UUID_VAL}-result.json" <<EOF
{"uuid":"$UUID_VAL","name":"$stage stage skipped","status":"skipped","stage":"finished","start":$TS,"stop":$TS}
EOF
  fi
done

allure generate "$OUT_RESULTS" -o "$OUT_REPORT" --clean

rm -rf "$HISTORY_DIR"
mkdir -p "$HISTORY_DIR"
if [ -d "$OUT_REPORT/history" ]; then
  cp -a "$OUT_REPORT/history/." "$HISTORY_DIR"
fi
