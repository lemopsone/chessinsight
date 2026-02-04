package ru.chessinsight.testdata;

import ru.chessinsight.infrastructure.persistence.jpa.model.TrainingScenarioEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public class TrainingScenarioEntityBuilder {
    private UUID id = UUID.randomUUID();
    private UUID userId = UUID.randomUUID();
    private UUID gameId = UUID.randomUUID();
    private String positionFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    private String pvSan = "e4 e5";
    private String pvUci = "e2e4 e7e5";
    private String prompt = "Find the best continuation.";
    private boolean completed;
    private OffsetDateTime completedAt;

    public static TrainingScenarioEntityBuilder scenarioEntity() {
        return new TrainingScenarioEntityBuilder();
    }

    public TrainingScenarioEntityBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public TrainingScenarioEntityBuilder withUserId(UUID userId) {
        this.userId = userId;
        return this;
    }

    public TrainingScenarioEntityBuilder withGameId(UUID gameId) {
        this.gameId = gameId;
        return this;
    }

    public TrainingScenarioEntityBuilder withPositionFEN(String positionFEN) {
        this.positionFEN = positionFEN;
        return this;
    }

    public TrainingScenarioEntityBuilder withPvSan(String pvSan) {
        this.pvSan = pvSan;
        return this;
    }

    public TrainingScenarioEntityBuilder withPvUci(String pvUci) {
        this.pvUci = pvUci;
        return this;
    }

    public TrainingScenarioEntityBuilder withPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    public TrainingScenarioEntityBuilder withCompleted(boolean completed) {
        this.completed = completed;
        return this;
    }

    public TrainingScenarioEntityBuilder withCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
        return this;
    }

    public TrainingScenarioEntity build() {
        TrainingScenarioEntity entity = new TrainingScenarioEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setGameId(gameId);
        entity.setPositionFEN(positionFEN);
        entity.setPvSan(pvSan);
        entity.setPvUci(pvUci);
        entity.setPrompt(prompt);
        entity.setCompleted(completed);
        entity.setCompletedAt(completedAt);
        return entity;
    }
}
