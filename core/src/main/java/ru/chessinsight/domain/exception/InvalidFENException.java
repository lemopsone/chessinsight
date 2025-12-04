package ru.chessinsight.domain.exception;

public final class InvalidFENException extends DomainException {
    public InvalidFENException(String msg) {
        super("Incorrect FEN: " + msg);
    }
}
