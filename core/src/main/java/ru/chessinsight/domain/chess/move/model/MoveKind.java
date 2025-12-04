package ru.chessinsight.domain.chess.move.model;

public enum MoveKind {
    NORMAL,
    PROMOTION,
    EN_PASSANT,
    CASTLE_KING_SIDE,
    CASTLE_QUEEN_SIDE,
    DOUBLE_PAWN_PUSH,
    ILLEGAL
}
