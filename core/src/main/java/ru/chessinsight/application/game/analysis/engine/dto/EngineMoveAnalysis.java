package ru.chessinsight.application.game.analysis.engine.dto;

import java.util.List;

public record EngineMoveAnalysis(
        String positionFEN,
        String playedMoveUci,
        String playedMoveSan,
        Integer evalCp,
        Integer mateScore,
        String bestMoveUci,
        String bestMoveSan,
        Integer cpLoss,
        List<String> bestPvUci,
        List<String> bestPvSan
) {}
