package ru.chessinsight.application.auth.exception;

public class UserExistsException extends  RuntimeException{
    public UserExistsException(String errorMessage) {
            super(errorMessage);
        }
}
