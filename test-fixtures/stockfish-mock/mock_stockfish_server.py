#!/usr/bin/env python3
import os
import socketserver

import chess


HOST = "0.0.0.0"
PORT = int(os.environ.get("PORT", "5555"))
STARTPOS_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1"


def bestmove_for_position(fen: str) -> tuple[str, str, int]:
    board = chess.Board(STARTPOS_FEN if fen == "startpos" else fen)
    legal = sorted((move.uci() for move in board.legal_moves))
    if not legal:
        return "0000", "0000", 0
    best = legal[0]
    cp = 18 if board.turn == chess.WHITE else -18
    return best, best, cp


class UciTcpHandler(socketserver.StreamRequestHandler):
    def setup(self):
        super().setup()
        self.current_fen = "startpos"

    def handle(self):
        while True:
            raw = self.rfile.readline()
            if not raw:
                return
            command = raw.decode("utf-8", errors="replace").strip()
            if not command:
                continue

            if command == "uci":
                self._send("id name ChessInsight Stockfish Mock")
                self._send("id author ChessInsight")
                self._send("uciok")
                continue

            if command == "isready":
                self._send("readyok")
                continue

            if command.startswith("setoption"):
                continue

            if command == "ucinewgame":
                self.current_fen = "startpos"
                continue

            if command.startswith("position fen "):
                self.current_fen = command[len("position fen "):]
                continue

            if command.startswith("go "):
                bestmove, pv, cp = bestmove_for_position(self.current_fen)
                self._send(f"info depth 8 seldepth 12 multipv 1 score cp {cp} pv {pv}")
                self._send(f"bestmove {bestmove}")
                continue

            if command in ("quit", "stop", "ponderhit"):
                if command == "quit":
                    return
                continue

    def _send(self, line: str):
        self.wfile.write((line + "\n").encode("utf-8"))
        self.wfile.flush()


class ThreadedTcpServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
    allow_reuse_address = True


if __name__ == "__main__":
    with ThreadedTcpServer((HOST, PORT), UciTcpHandler) as server:
        server.serve_forever()
