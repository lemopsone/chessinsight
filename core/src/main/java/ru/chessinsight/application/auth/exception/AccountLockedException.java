package ru.chessinsight.application.auth.exception;

public class AccountLockedException extends AuthException {
    public AccountLockedException(String errorMessage) {
        super(errorMessage);
    }
}
