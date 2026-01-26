package ru.chessinsight.application.game.dto;

import ru.chessinsight.domain.game.model.GameResult;

import java.time.LocalDate;

public record GameSearchCriteria(
        GameResult result,
        LocalDate dateFrom,
        LocalDate dateTo,
        Boolean analyzed
) {
}
