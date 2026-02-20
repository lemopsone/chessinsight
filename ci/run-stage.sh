#!/usr/bin/env bash

set -euo pipefail

STAGE=${1:-unit}
RESULTS_DIR=${2:-target/ci-reports}
TEST_DB_MODE=${TEST_DB_MODE:-testcontainers}
TEST_DB_URL=${TEST_DB_URL:-}
TEST_DB_USERNAME=${TEST_DB_USERNAME:-postgres}
TEST_DB_PASSWORD=${TEST_DB_PASSWORD:-postgres}
TEST_DB_SCHEMA_PREFIX=${TEST_DB_SCHEMA_PREFIX:-itest}
TEST_DB_SCHEMA_NAME=${TEST_DB_SCHEMA_NAME:-}
MAVEN_REPO_LOCAL=${MAVEN_REPO_LOCAL:-}
JACOCO_SKIP=${JACOCO_SKIP:-}
TEST_RANDOM_SEED=${TEST_RANDOM_SEED:-$(date +%s%N)}

if ! [[ "$TEST_RANDOM_SEED" =~ ^[0-9]+$ ]]; then
  TEST_RANDOM_SEED=$(printf '%s' "$TEST_RANDOM_SEED" | cksum | awk '{print $1}')
fi

if [ "${#TEST_RANDOM_SEED}" -gt 18 ]; then
  TEST_RANDOM_SEED="${TEST_RANDOM_SEED:0:18}"
fi

mkdir -p "$RESULTS_DIR"

EXTRA_ARGS=()
MAVEN_ARGS=()

MAVEN_ARGS+=("--batch-mode" "--no-transfer-progress")
MAVEN_ARGS+=("-Dtests.random.seed=${TEST_RANDOM_SEED}")
MAVEN_ARGS+=("-Djunit.jupiter.execution.order.random.seed=${TEST_RANDOM_SEED}")

if [ -n "$MAVEN_REPO_LOCAL" ]; then
  MAVEN_ARGS+=("-Dmaven.repo.local=${MAVEN_REPO_LOCAL}")
fi

if [ -n "$JACOCO_SKIP" ]; then
  MAVEN_ARGS+=("-Djacoco.skip=${JACOCO_SKIP}")
fi

echo "TEST_RANDOM_SEED=${TEST_RANDOM_SEED}"

if [ "$TEST_DB_MODE" = "local-shared" ]; then
  if [ -z "$TEST_DB_URL" ]; then
    echo "TEST_DB_URL is required for local-shared mode"
    exit 1
  fi
  EXTRA_ARGS+=(
    "-Dtest.db.mode=local-shared"
    "-Dtest.db.url=${TEST_DB_URL}"
    "-Dtest.db.username=${TEST_DB_USERNAME}"
    "-Dtest.db.password=${TEST_DB_PASSWORD}"
    "-Dtest.db.schema.prefix=${TEST_DB_SCHEMA_PREFIX}"
  )
  if [ -n "$TEST_DB_SCHEMA_NAME" ]; then
    EXTRA_ARGS+=("-Dtest.db.schema.name=${TEST_DB_SCHEMA_NAME}")
  fi
fi

set +e
case "$STAGE" in
  unit)
    mvn "${MAVEN_ARGS[@]}" -pl core,jpa,web -am test -Dallure.test.stage=unit
    STATUS=$?
    ;;
  integration)
    mvn "${MAVEN_ARGS[@]}" -pl jpa -am verify -DskipUnitTests=true -DskipITs=false -DskipE2E=true -Dallure.test.stage=integration "${EXTRA_ARGS[@]}"
    STATUS=$?
    ;;
  e2e)
    mvn "${MAVEN_ARGS[@]}" -pl web -am verify -DskipUnitTests=true -DskipITs=true -DskipE2E=false -Dallure.test.stage=e2e "${EXTRA_ARGS[@]}"
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
