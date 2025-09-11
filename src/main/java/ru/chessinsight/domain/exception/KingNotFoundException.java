package ru.chessinsight.domain.exception;

import ru.chessinsight.domain.chess.piece.model.Color;

public final class KingNotFoundException extends DomainException {
    public KingNotFoundException(Color color) {
        super("King for " + color.name() + " not found");
    }
}
