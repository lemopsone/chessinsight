package ru.chessinsight.application.game.dto;

public record MoveAnalysisDTO(
        String positionFEN,
        MoveDTO bestMove,
        Double bestMoveEval,
        Double playerMoveEval,
        Integer mateScore
) {}
