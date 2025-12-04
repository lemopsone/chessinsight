package ru.chessinsight.application.game.training.service.exception;

public class ScenarioCreationException extends RuntimeException {
    public ScenarioCreationException() { super(); }
    public ScenarioCreationException(String errorMessage) { super(errorMessage); }
}
