#!/usr/bin/env bash

set -euo pipefail

REPO_URL=${REPO_URL:-}
REPO_REF=${REPO_REF:-}
TEST_STAGE=${TEST_STAGE:-unit}
RESULTS_DIR=${RESULTS_DIR:-/reports}
WORKDIR=/workspace

if [ -z "$REPO_URL" ]; then
  echo "REPO_URL is required"
  exit 1
fi

git clone "$REPO_URL" "$WORKDIR"
cd "$WORKDIR"

if [ -n "$REPO_REF" ]; then
  git checkout "$REPO_REF"
fi

if [ "$TEST_STAGE" = "report" ]; then
  ./ci/generate-report.sh "$RESULTS_DIR"
else
  ./ci/run-stage.sh "$TEST_STAGE" "$RESULTS_DIR"
fi
