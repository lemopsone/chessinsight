package ru.chessinsight.domain.exception;

public abstract sealed class DomainException extends RuntimeException
        permits BadBoardCoordinatesException, BadFENCharException, InvalidFENException, InvalidSANException, InvalidStateException, InvalidUCIException, KingNotFoundException {
    public DomainException(String msg) { super(msg); }
}

