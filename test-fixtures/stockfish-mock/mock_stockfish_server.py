#!/usr/bin/env python3
import os
import socketserver


HOST = "0.0.0.0"
PORT = int(os.environ.get("PORT", "5555"))
STARTPOS_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1"


def parse_side_to_move(fen: str) -> str:
    parts = fen.split()
    if len(parts) > 1 and parts[1] in ("w", "b"):
        return parts[1]
    return "w"


def parse_board(fen: str) -> dict[str, str]:
    board_part = STARTPOS_FEN.split()[0] if fen == "startpos" else fen.split()[0]
    board: dict[str, str] = {}
    files = "abcdefgh"
    rank = 8
    file_idx = 0
    for ch in board_part:
        if ch == "/":
            rank -= 1
            file_idx = 0
            continue
        if ch.isdigit():
            file_idx += int(ch)
            continue
        square = f"{files[file_idx]}{rank}"
        board[square] = ch
        file_idx += 1
    return board


def bestmove_for_position(fen: str) -> tuple[str, str, int]:
    side = parse_side_to_move(fen)
    board = parse_board(fen)

    if side == "w":
        if board.get("e2") == "P":
            move = "e2e4"
        elif board.get("g1") == "N":
            move = "g1f3"
        elif board.get("d2") == "P":
            move = "d2d4"
        elif board.get("b1") == "N":
            move = "b1c3"
        else:
            move = "a2a3" if board.get("a2") == "P" else "h2h3"
        return move, move, 18

    if board.get("e7") == "p":
        move = "e7e5"
    elif board.get("g8") == "n":
        move = "g8f6"
    elif board.get("d7") == "p":
        move = "d7d5"
    elif board.get("b8") == "n":
        move = "b8c6"
    else:
        move = "a7a6" if board.get("a7") == "p" else "h7h6"
    return move, move, -18


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
