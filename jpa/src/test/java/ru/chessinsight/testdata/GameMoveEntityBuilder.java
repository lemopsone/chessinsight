package ru.chessinsight.testdata;

import ru.chessinsight.infrastructure.persistence.jpa.model.GameMoveEntity;

import java.util.UUID;

public class GameMoveEntityBuilder {
    private UUID id = UUID.randomUUID();
    private Integer plyIndex = 1;
    private String san = "e4";
    private String uci = "e2e4";
    private String positionFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    private String commentBefore;
    private String commentAfter;
    private Double analysisEvalCp = 0.2;
    private Integer analysisMateScore;
    private String analysisBestUci = "e2e4";
    private Double analysisCpLoss = 0.1;
    private String analysisCategory = "GOOD_MOVE";

    public static GameMoveEntityBuilder moveEntity() {
        return new GameMoveEntityBuilder();
    }

    public GameMoveEntityBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public GameMoveEntityBuilder withPlyIndex(Integer plyIndex) {
        this.plyIndex = plyIndex;
        return this;
    }

    public GameMoveEntityBuilder withSan(String san) {
        this.san = san;
        return this;
    }

    public GameMoveEntityBuilder withUci(String uci) {
        this.uci = uci;
        return this;
    }

    public GameMoveEntityBuilder withPositionFEN(String positionFEN) {
        this.positionFEN = positionFEN;
        return this;
    }

    public GameMoveEntityBuilder withCommentBefore(String commentBefore) {
        this.commentBefore = commentBefore;
        return this;
    }

    public GameMoveEntityBuilder withCommentAfter(String commentAfter) {
        this.commentAfter = commentAfter;
        return this;
    }

    public GameMoveEntityBuilder withAnalysisEvalCp(Double analysisEvalCp) {
        this.analysisEvalCp = analysisEvalCp;
        return this;
    }

    public GameMoveEntityBuilder withAnalysisMateScore(Integer analysisMateScore) {
        this.analysisMateScore = analysisMateScore;
        return this;
    }

    public GameMoveEntityBuilder withAnalysisBestUci(String analysisBestUci) {
        this.analysisBestUci = analysisBestUci;
        return this;
    }

    public GameMoveEntityBuilder withAnalysisCpLoss(Double analysisCpLoss) {
        this.analysisCpLoss = analysisCpLoss;
        return this;
    }

    public GameMoveEntityBuilder withAnalysisCategory(String analysisCategory) {
        this.analysisCategory = analysisCategory;
        return this;
    }

    public GameMoveEntity build() {
        GameMoveEntity entity = new GameMoveEntity();
        entity.setId(id);
        entity.setPlyIndex(plyIndex);
        entity.setSan(san);
        entity.setUci(uci);
        entity.setPositionFEN(positionFEN);
        entity.setCommentBefore(commentBefore);
        entity.setCommentAfter(commentAfter);
        entity.setAnalysisEvalCp(analysisEvalCp);
        entity.setAnalysisMateScore(analysisMateScore);
        entity.setAnalysisBestUci(analysisBestUci);
        entity.setAnalysisCpLoss(analysisCpLoss);
        entity.setAnalysisCategory(analysisCategory);
        return entity;
    }
}
