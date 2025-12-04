package ru.chessinsight.application.game.dto;

import ru.chessinsight.domain.game.model.GameResult;

import java.time.LocalDate;

public record GameMetadataDTO(
        String event,
        String site,
        LocalDate date,
        String round,
        String whiteName,
        String blackName,
        GameResult result
) {}
