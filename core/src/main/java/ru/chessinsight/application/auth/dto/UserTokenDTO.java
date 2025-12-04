package ru.chessinsight.application.auth.dto;

public record UserTokenDTO(
        String accessToken,
        String refreshToken,
        String tokenType
) {}
