package ru.chessinsight.application.auth.exception;

public class AuthException extends RuntimeException {
    public AuthException(String errorMessage) { super(errorMessage); }
}
