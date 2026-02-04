#!/usr/bin/env bash

set -euo pipefail

RESULTS_DIR=${1:-/reports}
OUT_RESULTS=${2:-$RESULTS_DIR/allure-results-merged}
OUT_REPORT=${3:-$RESULTS_DIR/allure-report}

rm -rf "$OUT_RESULTS" "$OUT_REPORT"
mkdir -p "$OUT_RESULTS"

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
