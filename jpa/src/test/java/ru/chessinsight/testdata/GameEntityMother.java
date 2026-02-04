package ru.chessinsight.testdata;

import ru.chessinsight.infrastructure.persistence.jpa.model.GameEntity;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public final class GameEntityMother {
    private GameEntityMother() {}

    public static GameEntity fullGameEntity() {
        return GameEntityBuilder.gameEntity()
                .withId(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .withUserId(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                .withEvent("Championship")
                .withSite("Online")
                .withDate(LocalDate.of(2024, 3, 3))
                .withRound("2")
                .withWhiteName("Alice")
                .withBlackName("Bob")
                .withResult("WHITE_WIN")
                .withPgn("1. e4 e5 *")
                .withAnalysis(GameAnalysisEntityBuilder.analysisEntity().build())
                .withMoves(Set.of(
                        GameMoveEntityBuilder.moveEntity().withPlyIndex(1).withSan("e4").withUci("e2e4").build(),
                        GameMoveEntityBuilder.moveEntity().withPlyIndex(2).withSan("e5").withUci("e7e5").build()
                ))
                .build();
    }
}
