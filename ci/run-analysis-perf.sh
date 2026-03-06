#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)

DEFAULT_OUTPUT_DIR="${ROOT_DIR}/reports/perf/observability"
OUTPUT_DIR=${OUTPUT_DIR:-$DEFAULT_OUTPUT_DIR}
RPS=${RPS:-100}
DURATION=${DURATION:-120s}
WARMUP_DURATION=${WARMUP_DURATION:-30s}
WARMUP_RPS=${WARMUP_RPS:-25}
SLEEP_AFTER_SCENARIO=${SLEEP_AFTER_SCENARIO:-6}
KEEP_UP=${KEEP_UP:-false}
UI_MODE=${UI_MODE:-auto}

ARGS=(
  --output-dir "$OUTPUT_DIR"
  --rps "$RPS"
  --duration "$DURATION"
  --warmup-duration "$WARMUP_DURATION"
  --warmup-rps "$WARMUP_RPS"
  --sleep-after-scenario "$SLEEP_AFTER_SCENARIO"
  --ui-mode "$UI_MODE"
)

if [ "$#" -gt 0 ]; then
  ARGS=("$@")
fi

if [ "$KEEP_UP" = "true" ]; then
  HAS_KEEP_UP=false
  for arg in "${ARGS[@]}"; do
    if [ "$arg" = "--keep-up" ]; then
      HAS_KEEP_UP=true
      break
    fi
  done
  if [ "$HAS_KEEP_UP" = "false" ]; then
    ARGS+=(--keep-up)
  fi
fi

python3 "$ROOT_DIR/ci/perf/run_observability_benchmark.py" "${ARGS[@]}"
