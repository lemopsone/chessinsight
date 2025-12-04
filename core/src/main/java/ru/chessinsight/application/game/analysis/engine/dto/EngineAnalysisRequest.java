package ru.chessinsight.application.game.analysis.engine.dto;

/** How to run the engine for a given position. One of depth/movetime/nodes should be set. */
public record EngineAnalysisRequest(
        String positionFEN,
        Integer depth,
        Integer movetimeMs,
        Integer nodes,
        Integer multipv,
        Integer pvLimit
) {
    public int effectiveDepth(int fallback) { return depth != null ? depth : fallback; }
    public int effectiveMultiPv() { return (multipv == null || multipv < 1) ? 1 : multipv; }
    public int effectivePvLimit(int fallback) { return pvLimit != null && pvLimit > 0 ? pvLimit : fallback; }
}
