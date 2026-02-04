package ru.chessinsight.testdata;

import ru.chessinsight.infrastructure.persistence.jpa.model.TrainingScenarioEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class TrainingScenarioEntityMother {
    private TrainingScenarioEntityMother() {}

    public static TrainingScenarioEntity completedScenarioEntity() {
        return TrainingScenarioEntityBuilder.scenarioEntity()
                .withId(UUID.fromString("55555555-5555-5555-5555-555555555555"))
                .withUserId(UUID.fromString("66666666-6666-6666-6666-666666666666"))
                .withGameId(UUID.fromString("77777777-7777-7777-7777-777777777777"))
                .withCompleted(true)
                .withCompletedAt(OffsetDateTime.now())
                .build();
    }
}
