package ru.chessinsight.application.game.analysis.engine.dto;

public record EngineMoveRequest(
        String positionFEN,
        String playedMoveUci,
        Integer depth,
        Integer movetimeMs,
        Integer nodes,
        Integer pvLimit
) {
    public int effectiveDepth(int fallback) { return depth != null ? depth : fallback; }
    public int effectivePvLimit(int fallback) { return pvLimit != null && pvLimit > 0 ? pvLimit : fallback; }
}
