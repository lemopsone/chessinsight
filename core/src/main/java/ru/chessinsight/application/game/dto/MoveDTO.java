package ru.chessinsight.application.game.dto;

public record MoveDTO(
        Long moveNum,
        String positionFEN,
        String moveSAN,
        String moveUCI
) {}
