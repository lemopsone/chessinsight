package ru.chessinsight.domain.chess.position.model;

import ru.chessinsight.domain.chess.piece.model.Bishop;
import ru.chessinsight.domain.chess.piece.model.Color;
import ru.chessinsight.domain.chess.piece.model.King;
import ru.chessinsight.domain.chess.piece.model.Knight;
import ru.chessinsight.domain.chess.piece.model.Pawn;
import ru.chessinsight.domain.chess.piece.model.Piece;
import ru.chessinsight.domain.chess.piece.model.Queen;
import ru.chessinsight.domain.chess.piece.model.Rook;
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
            int rank = 7 - rFen; // 8-я -> 7, 1-я -> 0
            fillRankFromFenRow(pieces, rows[rFen], rank);
        }

        return new Chessboard(pieces);
    }

    private static void fillRankFromFenRow(Map<BoardCoordinates, Piece> pieces, String row, int rank) {
        int file = 0;
        for (char symbol : row.toCharArray()) {
            if (Character.isDigit(symbol)) {
                file += (symbol - '0');
                continue;
            }
            pieces.put(new BoardCoordinates(rank, file), pieceFromFenChar(symbol));
            file++;
        }
    }

    private static Piece pieceFromFenChar(char symbol) {
        Color color = Character.isUpperCase(symbol) ? Color.WHITE : Color.BLACK;
        return switch (Character.toLowerCase(symbol)) {
            case 'k' -> new King(color);
            case 'q' -> new Queen(color);
            case 'r' -> new Rook(color);
            case 'b' -> new Bishop(color);
            case 'n' -> new Knight(color);
            case 'p' -> new Pawn(color);
            default -> throw new BadFENCharException(symbol);
        };
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
