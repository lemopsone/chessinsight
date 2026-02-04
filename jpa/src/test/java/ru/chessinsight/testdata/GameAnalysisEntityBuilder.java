package ru.chessinsight.testdata;

import ru.chessinsight.infrastructure.persistence.jpa.model.GameAnalysisEntity;

import java.time.OffsetDateTime;

public class GameAnalysisEntityBuilder {
    private Double accuracyWhite = 0.8;
    private Double accuracyBlack = 0.75;
    private Integer inaccuracies = 1;
    private Integer mistakes = 0;
    private Integer blunders = 0;
    private OffsetDateTime analyzedAt = OffsetDateTime.now();

    public static GameAnalysisEntityBuilder analysisEntity() {
        return new GameAnalysisEntityBuilder();
    }

    public GameAnalysisEntityBuilder withAccuracyWhite(Double accuracyWhite) {
        this.accuracyWhite = accuracyWhite;
        return this;
    }

    public GameAnalysisEntityBuilder withAccuracyBlack(Double accuracyBlack) {
        this.accuracyBlack = accuracyBlack;
        return this;
    }

    public GameAnalysisEntityBuilder withInaccuracies(Integer inaccuracies) {
        this.inaccuracies = inaccuracies;
        return this;
    }

    public GameAnalysisEntityBuilder withMistakes(Integer mistakes) {
        this.mistakes = mistakes;
        return this;
    }

    public GameAnalysisEntityBuilder withBlunders(Integer blunders) {
        this.blunders = blunders;
        return this;
    }

    public GameAnalysisEntityBuilder withAnalyzedAt(OffsetDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
        return this;
    }

    public GameAnalysisEntity build() {
        GameAnalysisEntity entity = new GameAnalysisEntity();
        entity.setAccuracyWhite(accuracyWhite);
        entity.setAccuracyBlack(accuracyBlack);
        entity.setInaccuracies(inaccuracies);
        entity.setMistakes(mistakes);
        entity.setBlunders(blunders);
        entity.setAnalyzedAt(analyzedAt);
        return entity;
    }
}
