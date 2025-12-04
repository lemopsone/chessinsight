package ru.chessinsight.domain.game.model;

import ru.chessinsight.application.game.analysis.model.MoveCategory;

public record GameMoveAnalysis(
        Double evalCp,
        Integer mateScore,
        String bestUCI,
        Double cpLoss,
        MoveCategory category
) {
}
