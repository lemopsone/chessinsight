package ru.chessinsight.infrastructure.cli.api.dto;

import java.util.List;
import java.util.UUID;

public class ApiGameAnalysis {
    private UUID gameId;
    private UUID userId;
    private Double accuracyWhite;
    private Double accuracyBlack;
    private List<ApiMoveAnalysis> bestMoves;
    private List<ApiMoveAnalysis> goodMoves;
    private List<ApiMoveAnalysis> inaccuracies;
    private List<ApiMoveAnalysis> mistakes;
    private List<ApiMoveAnalysis> blunders;

    public UUID getGameId() { return gameId; }
    public void setGameId(UUID gameId) { this.gameId = gameId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Double getAccuracyWhite() { return accuracyWhite; }
    public void setAccuracyWhite(Double accuracyWhite) { this.accuracyWhite = accuracyWhite; }

    public Double getAccuracyBlack() { return accuracyBlack; }
    public void setAccuracyBlack(Double accuracyBlack) { this.accuracyBlack = accuracyBlack; }

    public List<ApiMoveAnalysis> getBestMoves() { return bestMoves; }
    public void setBestMoves(List<ApiMoveAnalysis> bestMoves) { this.bestMoves = bestMoves; }

    public List<ApiMoveAnalysis> getGoodMoves() { return goodMoves; }
    public void setGoodMoves(List<ApiMoveAnalysis> goodMoves) { this.goodMoves = goodMoves; }

    public List<ApiMoveAnalysis> getInaccuracies() { return inaccuracies; }
    public void setInaccuracies(List<ApiMoveAnalysis> inaccuracies) { this.inaccuracies = inaccuracies; }

    public List<ApiMoveAnalysis> getMistakes() { return mistakes; }
    public void setMistakes(List<ApiMoveAnalysis> mistakes) { this.mistakes = mistakes; }

    public List<ApiMoveAnalysis> getBlunders() { return blunders; }
    public void setBlunders(List<ApiMoveAnalysis> blunders) { this.blunders = blunders; }
}
