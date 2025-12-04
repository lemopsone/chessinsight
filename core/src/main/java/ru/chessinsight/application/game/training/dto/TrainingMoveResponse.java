package ru.chessinsight.application.game.training.dto;

import java.util.List;

public record TrainingMoveResponse(
        Status status,
        String message,
        String acceptedMoveUci,
        String opponentMoveUci,
        int nextCursor,
        boolean completed,
        List<String> hintPvSan,
        List<String> hintPvUci
) {
    public enum Status { CONTINUE, COMPLETED, INCORRECT }
}
