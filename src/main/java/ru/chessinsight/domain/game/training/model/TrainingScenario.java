package ru.chessinsight.domain.game.training.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class TrainingScenario {
    private UUID id;
    private UUID userId;
    private UUID gameId;
    private String positionFEN;
    private String pvSan;
    private String pvUci;
    private String prompt;
    private boolean completed;
    private OffsetDateTime completedAt;

    public TrainingScenario() {}

    public TrainingScenario(UUID id, UUID userId, UUID gameId, String positionFEN, String pvSan, String pvUci, String prompt, boolean completed, OffsetDateTime completedAt) {
        this.id = id;
        this.userId = userId;
        this.gameId = gameId;
        this.positionFEN = positionFEN;
        this.pvSan = pvSan;
        this.pvUci = pvUci;
        this.prompt = prompt;
        this.completed = completed;
        this.completedAt = completedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getGameId() {
        return gameId;
    }

    public void setGameId(UUID gameId) {
        this.gameId = gameId;
    }

    public String getPositionFEN() {
        return positionFEN;
    }

    public void setPositionFEN(String positionFEN) {
        this.positionFEN = positionFEN;
    }

    public String getPvSan() {
        return pvSan;
    }

    public void setPvSan(String pvSan) {
        this.pvSan = pvSan;
    }

    public String getPvUci() {
        return pvUci;
    }

    public void setPvUci(String pvUci) {
        this.pvUci = pvUci;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
