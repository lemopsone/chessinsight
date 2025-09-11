package ru.chessinsight.domain.chess.move.model;

import ru.chessinsight.domain.chess.piece.model.Color;
import ru.chessinsight.domain.chess.position.model.BoardCoordinates;
import ru.chessinsight.domain.chess.piece.model.PromotionPieceType;

import java.util.Objects;

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

    public static Move castleKingSide(Color c) {
        int rank = c == Color.WHITE ? 1 : 8;
        return new Move(BoardCoordinates.fromString("e" + rank),
                BoardCoordinates.fromString("g" + rank),
                null,
                MoveKind.CASTLE_KING_SIDE, false);
    }

    public static Move castleQueenSide(Color c) {
        int rank = c == Color.WHITE ? 1 : 8;
        return new Move(BoardCoordinates.fromString("e" + rank),
                BoardCoordinates.fromString("c" + rank),
                null,
                MoveKind.CASTLE_QUEEN_SIDE, false);
    }

    public static Move doublePush(BoardCoordinates from, BoardCoordinates to) {
        return new Move(from, to, null, MoveKind.DOUBLE_PAWN_PUSH, false);
    }

    public static Move enPassant(BoardCoordinates from, BoardCoordinates to) {
        return new Move(from, to, null, MoveKind.EN_PASSANT, true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Move move = (Move) o;

        if (isCapture != move.isCapture) return false;
        if (!Objects.equals(from, move.from)) return false;
        if (!Objects.equals(to, move.to)) return false;
        if (promotionTo != move.promotionTo) return false;
        return kind == move.kind;
    }

    @Override
    public int hashCode() {
        int result = from != null ? from.hashCode() : 0;
        result = 31 * result + (to != null ? to.hashCode() : 0);
        result = 31 * result + (promotionTo != null ? promotionTo.hashCode() : 0);
        result = 31 * result + (kind != null ? kind.hashCode() : 0);
        result = 31 * result + (isCapture ? 1 : 0);
        return result;
    }
}
