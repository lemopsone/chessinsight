package ru.chessinsight.application.game.analysis.engine.dto;

import java.util.List;

public record EnginePositionAnalysis(
        String positionFEN,
        Integer depth,
        Integer selDepth,
        String bestMoveUci,
        String bestMoveSan,
        List<String> pvUci,
        List<String> pvSan,
        Integer scoreCp,
        Integer scoreMate,
        List<CandidateLine> candidates
) {}
