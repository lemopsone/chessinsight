package ru.chessinsight.testdata;

import ru.chessinsight.domain.game.training.model.TrainingScenario;

import java.time.OffsetDateTime;
import java.util.UUID;

public class TrainingScenarioBuilder {
    private UUID id = UUID.randomUUID();
    private UUID userId = UUID.randomUUID();
    private UUID gameId = UUID.randomUUID();
    private String positionFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    private String pvSan = "e4 e5";
    private String pvUci = "e2e4 e7e5";
    private String prompt = "Find the best continuation.";
    private boolean completed;
    private OffsetDateTime completedAt;

    public static TrainingScenarioBuilder scenario() {
        return new TrainingScenarioBuilder();
    }

    public TrainingScenarioBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public TrainingScenarioBuilder withUserId(UUID userId) {
        this.userId = userId;
        return this;
    }

    public TrainingScenarioBuilder withGameId(UUID gameId) {
        this.gameId = gameId;
        return this;
    }

    public TrainingScenarioBuilder withPositionFEN(String positionFEN) {
        this.positionFEN = positionFEN;
        return this;
    }

    public TrainingScenarioBuilder withPvSan(String pvSan) {
        this.pvSan = pvSan;
        return this;
    }

    public TrainingScenarioBuilder withPvUci(String pvUci) {
        this.pvUci = pvUci;
        return this;
    }

    public TrainingScenarioBuilder withPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    public TrainingScenarioBuilder withCompleted(boolean completed) {
        this.completed = completed;
        return this;
    }

    public TrainingScenarioBuilder withCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
        return this;
    }

    public TrainingScenario build() {
        return new TrainingScenario(id, userId, gameId, positionFEN, pvSan, pvUci, prompt, completed, completedAt);
    }
}
