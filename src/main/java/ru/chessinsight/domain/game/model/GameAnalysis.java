package ru.chessinsight.domain.game.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class GameAnalysis {
    private UUID gameId;
    private Double accuracyWhite;
    private Double accuracyBlack;
    private Integer inaccuracies;
    private Integer mistakes;
    private Integer blunders;
    private OffsetDateTime analyzedAt;

    public GameAnalysis() {}

    public UUID getGameId() { return gameId; }
    public void setGameId(UUID gameId) { this.gameId = gameId; }

    public OffsetDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(OffsetDateTime analyzedAt) { this.analyzedAt = analyzedAt; }

    public Double getAccuracyWhite() { return accuracyWhite; }
    public void setAccuracyWhite(Double accuracyWhite) { this.accuracyWhite = accuracyWhite; }

    public Double getAccuracyBlack() { return accuracyBlack; }
    public void setAccuracyBlack(Double accuracyBlack) { this.accuracyBlack = accuracyBlack; }

    public Integer getInaccuracies() { return inaccuracies; }
    public void setInaccuracies(Integer inaccuracies) { this.inaccuracies = inaccuracies; }

    public Integer getMistakes() { return mistakes; }
    public void setMistakes(Integer mistakes) { this.mistakes = mistakes; }

    public Integer getBlunders() { return blunders; }
    public void setBlunders(Integer blunders) { this.blunders = blunders; }
}
