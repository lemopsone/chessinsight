#!/bin/sh
set -e

if [ -z "$REPLICATION_HOST" ]; then
  echo "не указан REPLICATION_HOST" >&2
  exit 1
fi

mkdir -p "$PGDATA"

if [ ! -s "$PGDATA/PG_VERSION" ]; then
  export PGPASSWORD="$REPLICATION_PASSWORD"
  echo "Ожидаем главный сервер $REPLICATION_HOST:${REPLICATION_PORT:-5432}..."
  until pg_isready -h "$REPLICATION_HOST" -p "${REPLICATION_PORT:-5432}" -U "$REPLICATION_USER"; do
    sleep 2
  done

  echo "Запуск реплики..."
  rm -rf "$PGDATA"/*
  pg_basebackup -h "$REPLICATION_HOST" -p "${REPLICATION_PORT:-5432}" -D "$PGDATA" -U "$REPLICATION_USER" -v -P -R
  chown -R postgres:postgres "$PGDATA"
fi

exec /usr/local/bin/docker-entrypoint.sh postgres
