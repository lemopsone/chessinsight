package ru.chessinsight.infrastructure.cli.api.dto;

public record ApiUserToken(
        String accessToken,
        String refreshToken,
        String tokenType
) {}
