package ru.chessinsight.domain.user.model;

public record UserStatistics(
    Double accuracy,
    Double accuracyWhite,
    Double accuracyBlack
) {}
