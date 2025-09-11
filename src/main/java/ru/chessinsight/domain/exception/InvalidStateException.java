package ru.chessinsight.domain.exception;

public final class InvalidStateException extends DomainException {
    public InvalidStateException(String msg) {
        super("Invalid state: " + msg);
    }
}
