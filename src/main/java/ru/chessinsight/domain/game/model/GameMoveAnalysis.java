package ru.chessinsight.domain.game.model;

public record GameMoveAnalysis(
        Integer evalCp,
        Integer mateScore,
        String bestUCI,
        Integer cpLoss,
        String category
) {}
