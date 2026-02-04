#!/usr/bin/env bash

set -euo pipefail

OUT_RESULTS=target/allure-results-merged
OUT_REPORT=target/allure-report-merged

rm -rf "$OUT_RESULTS" "$OUT_REPORT"
mkdir -p "$OUT_RESULTS"

for dir in core/target/allure-results jpa/target/allure-results web/target/allure-results; do
  if [ -d "$dir" ]; then
    cp -a "$dir/." "$OUT_RESULTS"
  fi
done

allure generate "$OUT_RESULTS" -o "$OUT_REPORT" --clean
allure open "$OUT_REPORT"
