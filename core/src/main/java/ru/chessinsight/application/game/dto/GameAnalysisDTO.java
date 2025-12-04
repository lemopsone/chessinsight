package ru.chessinsight.application.game.dto;

import java.util.List;
import java.util.UUID;

public record GameAnalysisDTO(
        UUID gameId,
        UUID userId,
        Double accuracyWhite,
        Double accuracyBlack,
        List<MoveAnalysisDTO> bestMoves,
        List<MoveAnalysisDTO> goodMoves,
        List<MoveAnalysisDTO> inaccuracies,
        List<MoveAnalysisDTO> mistakes,
        List<MoveAnalysisDTO> blunders
) {}
