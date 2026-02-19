#!/usr/bin/env bash

set -euo pipefail

STAGE=${1:-integration}
TEST_RUNNER_IMAGE=${TEST_RUNNER_IMAGE:-chessinsight-tests:local}
TEST_DB_MODE=${TEST_DB_MODE:-local-shared}
TEST_DB_URL=${TEST_DB_URL:-jdbc:postgresql://pg-itest:5432/chessinsight_test}
TEST_DB_USERNAME=${TEST_DB_USERNAME:-postgres}
TEST_DB_PASSWORD=${TEST_DB_PASSWORD:-postgres}
TEST_DB_SCHEMA_PREFIX=${TEST_DB_SCHEMA_PREFIX:-itest}
DOCKER_NETWORK=${DOCKER_NETWORK:-chessinsight-itest-net}
RESULTS_DIR=${RESULTS_DIR:-/workspace/target/ci-reports-local}
M2_CACHE_VOLUME=${M2_CACHE_VOLUME:-chessinsight-m2-cache}
MAVEN_REPO_LOCAL=${MAVEN_REPO_LOCAL:-/m2/repository}
JAVA_TOOL_OPTIONS=${JAVA_TOOL_OPTIONS:--XX:+EnableDynamicAgentLoading -Xshare:off --sun-misc-unsafe-memory-access=allow}
JACOCO_SKIP=${JACOCO_SKIP:-true}

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
  -e TEST_DB_SCHEMA_PREFIX="$TEST_DB_SCHEMA_PREFIX" \
  -e MAVEN_REPO_LOCAL="$MAVEN_REPO_LOCAL" \
  -e JACOCO_SKIP="$JACOCO_SKIP" \
  -e JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS" \
  -v "$M2_CACHE_VOLUME":/m2 \
  -v "$PWD":/workspace \
  -w /workspace \
  --entrypoint bash \
  "$TEST_RUNNER_IMAGE" \
  -lc "./ci/run-stage.sh $STAGE $RESULTS_DIR"
