package ru.chessinsight.domain.exception;

import ru.chessinsight.domain.exception.DomainException;

public final class InvalidSANException extends DomainException {
    public InvalidSANException(String msg) {
        super("Invalid SAN: " + msg);
    }
}
