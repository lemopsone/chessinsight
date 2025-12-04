package ru.chessinsight.application.auth.dto;

public record SignInDTO(
        String loginOrEmail,
        String passwordOrToken,
        AuthType authType
) {}
