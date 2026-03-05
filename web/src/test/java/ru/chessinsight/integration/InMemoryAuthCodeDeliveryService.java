package ru.chessinsight.integration;

import ru.chessinsight.application.auth.service.AuthCodeDeliveryService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAuthCodeDeliveryService implements AuthCodeDeliveryService {

    private final Map<String, String> otpCodesByEmail = new ConcurrentHashMap<>();
    private final Map<String, String> recoveryCodesByEmail = new ConcurrentHashMap<>();

    @Override
    public void sendOtpCode(String email, String code, int ttlSeconds) {
        otpCodesByEmail.put(email, code);
    }

    @Override
    public void sendRecoveryCode(String email, String code, int ttlSeconds) {
        recoveryCodesByEmail.put(email, code);
    }

    public String latestOtpCode(String email) {
        return otpCodesByEmail.get(email);
    }

    public String latestRecoveryCode(String email) {
        return recoveryCodesByEmail.get(email);
    }

    public void clearForEmail(String email) {
        otpCodesByEmail.remove(email);
        recoveryCodesByEmail.remove(email);
    }
}
