package ru.chessinsight.application.game.training.service.exception;

public class ScenarioNotFoundException extends RuntimeException {
    public ScenarioNotFoundException(String message) {
        super(message);
    }
}
