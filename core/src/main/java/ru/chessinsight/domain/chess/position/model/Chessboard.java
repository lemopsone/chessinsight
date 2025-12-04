package ru.chessinsight.domain.chess.position.model;

import ru.chessinsight.domain.chess.piece.model.*;
import ru.chessinsight.domain.exception.BadFENCharException;
import ru.chessinsight.domain.exception.KingNotFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class Chessboard {
    private final Map<BoardCoordinates, Piece> pieces;

    private Chessboard(Map<BoardCoordinates, Piece> pieces) {
        this.pieces = Map.copyOf(pieces);
    }

    private BoardCoordinates enPassantSquare;

    public boolean inside(BoardCoordinates coords) {
        return 0 <= coords.rank() && coords.rank() < 8 && 0 <= coords.file() && coords.file() < 8;
    }

    public BoardCoordinates getEnPassantSquare() { return enPassantSquare; }
    public void setEnPassantSquare(BoardCoordinates enPassantSquare) { this.enPassantSquare = enPassantSquare; }

    public Map<BoardCoordinates, Piece> getPieces() { return this.pieces; }
    public Piece at(BoardCoordinates coords) { return pieces.get(coords); }

    public Map<BoardCoordinates, Piece> getPiecesByColor(Color c) {
        return pieces.entrySet()
                .stream().filter(entry -> entry.getValue().getColor() == c)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public BoardCoordinates getKingSquare(Color c) {
        for (var entry : pieces.entrySet()) {
            if (entry.getValue() instanceof King king && king.getColor() == c) return entry.getKey();
        }
        throw new KingNotFoundException(c);
    }

    public static Chessboard empty() {
        return new Chessboard(Map.of());
    }

    public Chessboard withPiece(BoardCoordinates sq, Piece p) {
        var copy = new HashMap<>(pieces);
        copy.put(sq, p);
        return new Chessboard(copy);
    }

    public Chessboard without(BoardCoordinates sq) {
        if (!pieces.containsKey(sq)) return this;
        var copy = new HashMap<>(pieces);
        copy.remove(sq);
        return new Chessboard(copy);
    }

    public static Chessboard fromFENPlacement(String FEN) {
        Map<BoardCoordinates, Piece> pieces = new HashMap<>();
        String[] rows = FEN.split("/");
        for (int rFen = 0; rFen < 8; rFen++) {
            int rank = 7 - rFen;          // 8-я -> 7, 1-я -> 0
            int file = 0;
            for (char c : rows[rFen].toCharArray()) {
                if (Character.isDigit(c)) {
                    file += (c - '0');
                } else {
                    Color color = Character.isUpperCase(c) ? Color.WHITE : Color.BLACK;
                    Piece piece = switch (Character.toLowerCase(c)) {
                        case 'k' -> new King(color);
                        case 'q' -> new Queen(color);
                        case 'r' -> new Rook(color);
                        case 'b' -> new Bishop(color);
                        case 'n' -> new Knight(color);
                        case 'p' -> new Pawn(color);
                        default -> throw new BadFENCharException(c);
                    };
                    BoardCoordinates currentCoords = new BoardCoordinates(rank, file);
                    pieces.put(currentCoords, piece);
                    file++;
                }
            }
        }

        return new Chessboard(pieces);
    }

    public String toFENPlacement() {
        StringBuilder sb = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--) {
            int emptySquares = 0;
            for (int file = 0; file < 8; file++) {
                Piece piece = at(new BoardCoordinates(rank, file));
                if (piece == null)
                    emptySquares++;
                else {
                    if (emptySquares > 0) {
                        sb.append(emptySquares);
                        emptySquares = 0;
                    }
                    sb.append(piece.FENChar());
                }
            }
            if (emptySquares > 0)
                sb.append(emptySquares);
            if (rank > 0)
                sb.append('/');
        }

        return sb.toString();
    }
}
