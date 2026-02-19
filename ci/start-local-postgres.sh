#!/usr/bin/env bash

set -euo pipefail

DOCKER_NETWORK=${DOCKER_NETWORK:-chessinsight-itest-net}
DB_CONTAINER_NAME=${DB_CONTAINER_NAME:-chessinsight-it-postgres}
DB_IMAGE=${DB_IMAGE:-postgres:16-alpine}
DB_HOST_ALIAS=${DB_HOST_ALIAS:-pg-itest}
DB_PORT=${DB_PORT:-55432}
TEST_DB_NAME=${TEST_DB_NAME:-chessinsight_test}
TEST_DB_USERNAME=${TEST_DB_USERNAME:-postgres}
TEST_DB_PASSWORD=${TEST_DB_PASSWORD:-postgres}

if ! docker network inspect "$DOCKER_NETWORK" >/dev/null 2>&1; then
  docker network create "$DOCKER_NETWORK" >/dev/null
fi

if docker container inspect "$DB_CONTAINER_NAME" >/dev/null 2>&1; then
  RUNNING=$(docker inspect -f '{{.State.Running}}' "$DB_CONTAINER_NAME")
  if [ "$RUNNING" != "true" ]; then
    docker start "$DB_CONTAINER_NAME" >/dev/null
  fi
else
  docker run -d \
    --name "$DB_CONTAINER_NAME" \
    --network "$DOCKER_NETWORK" \
    --network-alias "$DB_HOST_ALIAS" \
    -e POSTGRES_DB="$TEST_DB_NAME" \
    -e POSTGRES_USER="$TEST_DB_USERNAME" \
    -e POSTGRES_PASSWORD="$TEST_DB_PASSWORD" \
    -p "${DB_PORT}:5432" \
    --health-cmd "pg_isready -U $TEST_DB_USERNAME -d $TEST_DB_NAME" \
    --health-interval 2s \
    --health-timeout 2s \
    --health-retries 30 \
    "$DB_IMAGE" >/dev/null
fi

for _ in $(seq 1 60); do
  STATUS=$(docker inspect -f '{{.State.Health.Status}}' "$DB_CONTAINER_NAME" 2>/dev/null || echo "starting")
  if [ "$STATUS" = "healthy" ]; then
    break
  fi
  sleep 1
done

STATUS=$(docker inspect -f '{{.State.Health.Status}}' "$DB_CONTAINER_NAME")
if [ "$STATUS" != "healthy" ]; then
  echo "Postgres container is not healthy: $STATUS"
  exit 1
fi

echo "Postgres is ready:"
echo "  Container: $DB_CONTAINER_NAME"
echo "  Network:   $DOCKER_NETWORK"
echo "  Internal:  jdbc:postgresql://$DB_HOST_ALIAS:5432/$TEST_DB_NAME"
echo "  Host:      jdbc:postgresql://localhost:$DB_PORT/$TEST_DB_NAME"
