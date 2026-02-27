#!/usr/bin/env bash

set -euo pipefail

STAGE=${1:-integration}
TEST_RUNNER_IMAGE=${TEST_RUNNER_IMAGE:-chessinsight-tests:local}
TEST_DB_MODE=${TEST_DB_MODE:-local-shared}
TEST_DB_URL=${TEST_DB_URL:-jdbc:postgresql://pg-itest:5432/chessinsight_test}
TEST_DB_USERNAME=${TEST_DB_USERNAME:-postgres}
TEST_DB_PASSWORD=${TEST_DB_PASSWORD:-postgres}
TEST_DB_NAME=${TEST_DB_NAME:-chessinsight_test}
TEST_DB_SCHEMA_PREFIX=${TEST_DB_SCHEMA_PREFIX:-itest}
TEST_DB_SCHEMA_NAME=${TEST_DB_SCHEMA_NAME:-}
DOCKER_NETWORK=${DOCKER_NETWORK:-chessinsight-itest-net}
DB_CONTAINER_NAME=${DB_CONTAINER_NAME:-chessinsight-it-postgres}
RESULTS_DIR=${RESULTS_DIR:-/workspace/target/ci-reports-local}
M2_CACHE_VOLUME=${M2_CACHE_VOLUME:-chessinsight-m2-cache}
MAVEN_REPO_LOCAL=${MAVEN_REPO_LOCAL:-/m2/repository}
JAVA_TOOL_OPTIONS=${JAVA_TOOL_OPTIONS:--XX:+EnableDynamicAgentLoading -Xshare:off --sun-misc-unsafe-memory-access=allow}
JACOCO_SKIP=${JACOCO_SKIP:-true}
TEST_STOCKFISH_PROVIDER=${TEST_STOCKFISH_PROVIDER:-mock}
TEST_RUN_ID=${TEST_RUN_ID:-$(cat /proc/sys/kernel/random/uuid | tr -d '-')}

if [ -z "$TEST_DB_SCHEMA_NAME" ]; then
  TEST_DB_SCHEMA_NAME="${TEST_DB_SCHEMA_PREFIX}_${TEST_RUN_ID}"
fi
TEST_DB_SCHEMA_NAME=$(echo "$TEST_DB_SCHEMA_NAME" | tr '[:upper:]' '[:lower:]')
if [[ ! "$TEST_DB_SCHEMA_NAME" =~ ^[a-z0-9_]+$ ]]; then
  echo "Invalid TEST_DB_SCHEMA_NAME: $TEST_DB_SCHEMA_NAME"
  exit 1
fi

case "$STAGE" in
  integration|e2e)
    ;;
  *)
    echo "Unknown stage: $STAGE"
    echo "Allowed values: integration, e2e"
    exit 1
    ;;
esac

docker network inspect "$DOCKER_NETWORK" >/dev/null 2>&1 || {
  echo "Docker network '$DOCKER_NETWORK' not found"
  echo "Run: ./ci/start-local-postgres.sh"
  exit 1
}

docker build -f ci/test-runner.Dockerfile -t "$TEST_RUNNER_IMAGE" .

docker volume inspect "$M2_CACHE_VOLUME" >/dev/null 2>&1 || docker volume create "$M2_CACHE_VOLUME" >/dev/null

HOST_UID=$(id -u)
HOST_GID=$(id -g)

cleanup_schema() {
  if [ "$TEST_DB_MODE" != "local-shared" ]; then
    return
  fi
  if ! docker container inspect "$DB_CONTAINER_NAME" >/dev/null 2>&1; then
    return
  fi
  docker exec "$DB_CONTAINER_NAME" \
    psql -U "$TEST_DB_USERNAME" -d "$TEST_DB_NAME" -v ON_ERROR_STOP=1 \
    -c "DROP SCHEMA IF EXISTS \"$TEST_DB_SCHEMA_NAME\" CASCADE;" >/dev/null 2>&1 || true
}

trap cleanup_schema EXIT

docker run --rm \
  -v "$M2_CACHE_VOLUME":/m2 \
  --entrypoint bash \
  "$TEST_RUNNER_IMAGE" \
  -lc "mkdir -p \"$MAVEN_REPO_LOCAL\" && chown -R ${HOST_UID}:${HOST_GID} /m2"

docker run --rm \
  --network "$DOCKER_NETWORK" \
  -u "${HOST_UID}:${HOST_GID}" \
  -e TEST_DB_MODE="$TEST_DB_MODE" \
  -e TEST_DB_URL="$TEST_DB_URL" \
  -e TEST_DB_USERNAME="$TEST_DB_USERNAME" \
  -e TEST_DB_PASSWORD="$TEST_DB_PASSWORD" \
  -e TEST_DB_NAME="$TEST_DB_NAME" \
  -e TEST_DB_SCHEMA_PREFIX="$TEST_DB_SCHEMA_PREFIX" \
  -e TEST_DB_SCHEMA_NAME="$TEST_DB_SCHEMA_NAME" \
  -e MAVEN_REPO_LOCAL="$MAVEN_REPO_LOCAL" \
  -e JACOCO_SKIP="$JACOCO_SKIP" \
  -e TEST_STOCKFISH_PROVIDER="$TEST_STOCKFISH_PROVIDER" \
  -e JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS" \
  -v "$M2_CACHE_VOLUME":/m2 \
  -v "$PWD":/workspace \
  -w /workspace \
  --entrypoint bash \
  "$TEST_RUNNER_IMAGE" \
  -lc "./ci/run-stage.sh $STAGE $RESULTS_DIR"
