#!/usr/bin/env bash

set -euo pipefail

MODE=${1:-real}
STOCKFISH_PORT=${ENGINE_STOCKFISH_PORT:-5555}

case "$MODE" in
  real)
    STOCKFISH_HOST=${ENGINE_STOCKFISH_HOST:-stockfish-pool}
    ENGINE_STOCKFISH_HOST="$STOCKFISH_HOST" \
    ENGINE_STOCKFISH_PORT="$STOCKFISH_PORT" \
      docker compose up -d
    ;;
  mock)
    STOCKFISH_HOST=${ENGINE_STOCKFISH_HOST:-stockfish-mock}
    ENGINE_STOCKFISH_HOST="$STOCKFISH_HOST" \
    ENGINE_STOCKFISH_PORT="$STOCKFISH_PORT" \
      docker compose --profile mock up -d
    ;;
  *)
    echo "Unknown mode: $MODE"
    echo "Usage: $0 [real|mock]"
    exit 1
    ;;
esac

echo "Environment started in '$MODE' mode"
echo "Stockfish host: $STOCKFISH_HOST"
echo "Stockfish port: $STOCKFISH_PORT"
