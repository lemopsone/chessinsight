package ru.chessinsight.application.auth.service;

public interface AuthCodeGeneratorService {
    String generateOtpCode();
    String generateRecoveryCode();
}
