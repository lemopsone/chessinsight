package ru.chessinsight.application.auth.dto;

public record RecoveryConfirmDTO(
        String loginOrEmail,
        String recoveryCode,
        String newPassword
) {}
