package ru.chessinsight.application.game.exception;

import ru.chessinsight.application.exception.ApplicationException;

public class GameNotFoundException extends ApplicationException {
    public GameNotFoundException(String msg) {
        super("Game not found");
    }
}
