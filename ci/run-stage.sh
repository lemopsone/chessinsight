#!/usr/bin/env bash

set -euo pipefail

STAGE=${1:-unit}
RESULTS_DIR=${2:-target/ci-reports}

mkdir -p "$RESULTS_DIR"

set +e
case "$STAGE" in
  unit)
    mvn -pl core,jpa,web -am test
    STATUS=$?
    ;;
  integration)
    mvn -pl jpa -am verify -DskipUnitTests=true -DskipITs=false -DskipE2E=true
    STATUS=$?
    ;;
  e2e)
    mvn -pl web -am verify -DskipUnitTests=true -DskipITs=true -DskipE2E=false
    STATUS=$?
    ;;
  *)
    echo "Unknown stage: $STAGE"
    exit 1
    ;;
esac
set -e

OUT_DIR="$RESULTS_DIR/allure-results-$STAGE"
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

for dir in core/target/allure-results jpa/target/allure-results web/target/allure-results; do
  if [ -d "$dir" ]; then
    cp -a "$dir/." "$OUT_DIR"
  fi
done

exit $STATUS
