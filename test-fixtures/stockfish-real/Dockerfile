FROM ubuntu:24.04

RUN apt-get update \
    && apt-get install -y --no-install-recommends stockfish socat \
    && rm -rf /var/lib/apt/lists/*

EXPOSE 5555
CMD ["socat", "TCP-LISTEN:5555,reuseaddr,fork", "EXEC:/usr/games/stockfish"]
