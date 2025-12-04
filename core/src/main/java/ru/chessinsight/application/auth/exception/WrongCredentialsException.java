package ru.chessinsight.application.auth.exception;

public class WrongCredentialsException extends RuntimeException{
    public WrongCredentialsException(String errorMessage) { super(errorMessage); }
}
