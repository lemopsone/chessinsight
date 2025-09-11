package ru.chessinsight.domain.game.model;

import java.util.List;
import java.util.UUID;

public class GameMove {
    private UUID gameId;
    private int plyIndex;
    private String san;
    private String uci;
    private String positionFEN;
    private String commentBefore;
    private String commentAfter;

    private GameMoveAnalysis analysis;

    public GameMove() {}

    public GameMove(UUID gameId, int plyIndex, String san, String uci, String positionFEN, String commentBefore, String commentAfter, GameMoveAnalysis analysis) {
        this.gameId = gameId;
        this.plyIndex = plyIndex;
        this.san = san;
        this.uci = uci;
        this.positionFEN = positionFEN;
        this.commentBefore = commentBefore;
        this.commentAfter = commentAfter;
        this.analysis = analysis;
    }

    public String getPositionFEN() {
        return positionFEN;
    }

    public void setPositionFEN(String positionFEN) {
        this.positionFEN = positionFEN;
    }

    public void setCommentBefore(String commentBefore) {
        this.commentBefore = commentBefore;
    }

    public void setCommentAfter(String commentAfter) {
        this.commentAfter = commentAfter;
    }

    public UUID getGameId() {
        return gameId;
    }

    public void setGameId(UUID gameId) {
        this.gameId = gameId;
    }

    public int getPlyIndex() {
        return plyIndex;
    }

    public void setPlyIndex(int plyIndex) {
        this.plyIndex = plyIndex;
    }

    public String getSan() {
        return san;
    }

    public void setSan(String san) {
        this.san = san;
    }

    public String getUci() {
        return uci;
    }

    public void setUci(String uci) {
        this.uci = uci;
    }

    public String getCommentBefore() {
        return commentBefore;
    }

    public String getCommentAfter() {
        return commentAfter;
    }

    public GameMoveAnalysis getAnalysis() {
        return analysis;
    }

    public void setAnalysis(GameMoveAnalysis analysis) {
        this.analysis = analysis;
    }
}
