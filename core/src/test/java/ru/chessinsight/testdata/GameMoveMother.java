package ru.chessinsight.testdata;

import ru.chessinsight.domain.game.model.GameMove;

public final class GameMoveMother {
    private GameMoveMother() {}

    public static GameMove simpleMove() {
        return GameMoveBuilder.move().build();
    }
}
