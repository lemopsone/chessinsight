package ru.chessinsight.domain.exception;

import ru.chessinsight.domain.exception.DomainException;

public final class BadFENCharException extends DomainException {
    public BadFENCharException(Character c) {
        super("Bad FEN char: " + c);
    }
}
