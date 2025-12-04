package ru.chessinsight.application.game.dto;

public record SanMoveDTO(
        Long moveNum,
        String positionFEN,
        String moveSAN
) {}
