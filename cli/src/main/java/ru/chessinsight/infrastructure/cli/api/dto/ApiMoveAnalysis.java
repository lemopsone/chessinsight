package ru.chessinsight.infrastructure.cli.api.dto;

public class ApiMoveAnalysis {
    private String positionFEN;
    private String bestMoveSan;
    private String bestMoveUci;
    private Integer bestMoveEval;
    private Integer playerMoveEval;
    private Integer mateScore;

    public String getPositionFEN() { return positionFEN; }
    public void setPositionFEN(String positionFEN) { this.positionFEN = positionFEN; }

    public String getBestMoveSan() { return bestMoveSan; }
    public void setBestMoveSan(String bestMoveSan) { this.bestMoveSan = bestMoveSan; }

    public String getBestMoveUci() { return bestMoveUci; }
    public void setBestMoveUci(String bestMoveUci) { this.bestMoveUci = bestMoveUci; }

    public Integer getBestMoveEval() { return bestMoveEval; }
    public void setBestMoveEval(Integer bestMoveEval) { this.bestMoveEval = bestMoveEval; }

    public Integer getPlayerMoveEval() { return playerMoveEval; }
    public void setPlayerMoveEval(Integer playerMoveEval) { this.playerMoveEval = playerMoveEval; }

    public Integer getMateScore() { return mateScore; }
    public void setMateScore(Integer mateScore) { this.mateScore = mateScore; }
}
