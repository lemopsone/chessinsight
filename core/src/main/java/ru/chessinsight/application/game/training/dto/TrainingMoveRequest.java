package ru.chessinsight.application.game.training.dto;

import java.util.UUID;

public record TrainingMoveRequest(
        UUID scenarioId,
        int cursor,
        String moveUCI,
        boolean isDemo
) {}
