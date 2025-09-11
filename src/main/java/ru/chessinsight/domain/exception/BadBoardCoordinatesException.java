package ru.chessinsight.domain.exception;

import ru.chessinsight.domain.exception.DomainException;

public final class BadBoardCoordinatesException extends DomainException {
    public BadBoardCoordinatesException(String msg) {
        super("Error when building BoardCoordinates: " + msg);
    }
}
