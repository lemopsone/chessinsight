package ru.chessinsight.domain.exception;

public final class InvalidUCIException extends DomainException {
    public InvalidUCIException(String msg) {
        super("Invalid UCI: " + msg);
    }
}
