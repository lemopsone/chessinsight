package ru.chessinsight.testdata;

import ru.chessinsight.domain.game.model.GameAnalysis;

import java.time.OffsetDateTime;

public class GameAnalysisBuilder {
    private Double accuracyWhite = 80.0;
    private Double accuracyBlack = 75.0;
    private Integer inaccuracies = 1;
    private Integer mistakes = 0;
    private Integer blunders = 0;
    private OffsetDateTime analyzedAt = OffsetDateTime.now();

    public static GameAnalysisBuilder analysis() {
        return new GameAnalysisBuilder();
    }

    public GameAnalysisBuilder withAccuracyWhite(Double accuracyWhite) {
        this.accuracyWhite = accuracyWhite;
        return this;
    }

    public GameAnalysisBuilder withAccuracyBlack(Double accuracyBlack) {
        this.accuracyBlack = accuracyBlack;
        return this;
    }

    public GameAnalysisBuilder withInaccuracies(Integer inaccuracies) {
        this.inaccuracies = inaccuracies;
        return this;
    }

    public GameAnalysisBuilder withMistakes(Integer mistakes) {
        this.mistakes = mistakes;
        return this;
    }

    public GameAnalysisBuilder withBlunders(Integer blunders) {
        this.blunders = blunders;
        return this;
    }

    public GameAnalysisBuilder withAnalyzedAt(OffsetDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
        return this;
    }

    public GameAnalysis build() {
        GameAnalysis analysis = new GameAnalysis();
        analysis.setAccuracyWhite(accuracyWhite);
        analysis.setAccuracyBlack(accuracyBlack);
        analysis.setInaccuracies(inaccuracies);
        analysis.setMistakes(mistakes);
        analysis.setBlunders(blunders);
        analysis.setAnalyzedAt(analyzedAt);
        return analysis;
    }
}
