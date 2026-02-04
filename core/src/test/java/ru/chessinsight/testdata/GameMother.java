package ru.chessinsight.testdata;

import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.model.GameResult;

import java.time.LocalDate;
import java.util.Set;

public final class GameMother {
    private GameMother() {}

    public static Game analyzedGame() {
        return GameBuilder.game()
                .withResult(GameResult.WHITE_WIN)
                .withDate(LocalDate.of(2024, 2, 2))
                .withAnalysis(GameAnalysisBuilder.analysis().build())
                .build();
    }

    public static Game emptyGame() {
        return GameBuilder.game()
                .withMoves(Set.of())
                .build();
    }
}
