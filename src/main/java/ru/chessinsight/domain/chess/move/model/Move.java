package ru.chessinsight.domain.chess.move.model;

import ru.chessinsight.domain.chess.position.model.BoardCoordinates;
import ru.chessinsight.domain.chess.piece.model.PromotionPieceType;

public record Move (
    BoardCoordinates from,
    BoardCoordinates to,
    PromotionPieceType promotionTo,
    MoveKind kind,
    boolean isCapture
) {
    public static Move move(BoardCoordinates from, BoardCoordinates to, boolean isCapture) {
        return new Move(from, to, null, MoveKind.NORMAL, isCapture);
    }

    public static Move promote(BoardCoordinates from, BoardCoordinates to,
                               PromotionPieceType newType, boolean isCapture) {
        return new Move(from, to, newType,  MoveKind.PROMOTION, isCapture);
    }

    public static Move castleKingSide() {
        return new Move(null, null, null,
                MoveKind.CASTLE_KING_SIDE, false);
    }

    public static Move castleQueenSide() {
        return new Move(null, null, null,
                MoveKind.CASTLE_QUEEN_SIDE, false);
    }

    public static Move doublePush(BoardCoordinates from, BoardCoordinates to) {
        return new Move(from, to, null, MoveKind.DOUBLE_PAWN_PUSH, false);
    }

    public static Move enPassant(BoardCoordinates from, BoardCoordinates to) {
        return new Move(from, to, null, MoveKind.EN_PASSANT, true);
    }
}
