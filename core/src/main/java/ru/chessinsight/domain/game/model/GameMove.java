package ru.chessinsight.domain.game.model;

import java.util.UUID;

public class GameMove {
    private UUID id;
    private int plyIndex;
    private String san;
    private String uci;
    private String positionFEN;
    private String commentBefore;
    private String commentAfter;

    private GameMoveAnalysis analysis;

    public GameMove() {}

    public GameMove(UUID id, int plyIndex, String san, String uci, String positionFEN, String commentBefore, String commentAfter, GameMoveAnalysis analysis) {
        this.id = id;
        this.plyIndex = plyIndex;
        this.san = san;
        this.uci = uci;
        this.positionFEN = positionFEN;
        this.commentBefore = commentBefore;
        this.commentAfter = commentAfter;
        this.analysis = analysis;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
