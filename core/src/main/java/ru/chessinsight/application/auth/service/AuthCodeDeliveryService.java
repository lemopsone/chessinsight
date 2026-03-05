package ru.chessinsight.application.auth.service;

public interface AuthCodeDeliveryService {
    void sendOtpCode(String email, String code, int ttlSeconds);
    void sendRecoveryCode(String email, String code, int ttlSeconds);
}
