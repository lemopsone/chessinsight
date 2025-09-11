package ru.chessinsight.domain.chess.position.model;

import ru.chessinsight.domain.chess.piece.model.Color;
import ru.chessinsight.domain.exception.DomainException;
import ru.chessinsight.domain.exception.InvalidFENException;

public record Position (
    Chessboard board,
    Color sideToMove,
    CastlingRights castlingRights,
    int turnNumber,
    int fiftyMoveRuleCounter) {

    public String toFEN() {
        String placement = board.toFENPlacement();
        String stm = sideToMove == Color.WHITE ? "w" : "b";
        String rights = castlingRights.toFEN();
        var epCoords = board.getEnPassantSquare();
        String enPassantString = (epCoords == null) ? "-" : epCoords.toString();
        return placement + " "
                + stm + " "
                + rights + " "
                + enPassantString + " "
                + fiftyMoveRuleCounter + " "
                + turnNumber;
    }

    public static Position fromFEN(String FEN) {
        String[] parts = FEN.trim().split("\\s+");
        if (parts.length != 6)
            throw new InvalidFENException("expected 6 parts, received" + parts.length);
        String placement = parts[0];
        try {
            Chessboard board = Chessboard.fromFENPlacement(placement);
            Color stm = parts[1].equals("w") ? Color.WHITE : Color.BLACK;
            CastlingRights rights = CastlingRights.fromFEN(parts[2]);
            BoardCoordinates enPassant = parts[3].equals("-")
                    ? null
                    : BoardCoordinates.fromString(parts[3]);
            board.setEnPassantSquare(enPassant);
            int halfMoves = Integer.parseInt(parts[4]);
            int turnNum = Integer.parseInt(parts[5]);
            return new Position(board, stm, rights, turnNum, halfMoves);
        } catch (DomainException|NumberFormatException e) {
           throw new InvalidFENException(e.getMessage());
        }
    }

    public long key() {
        return toFEN().hashCode();
    }

    public static Position initial() {
        String initialFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        return Position.fromFEN(initialFEN);
    }
}
