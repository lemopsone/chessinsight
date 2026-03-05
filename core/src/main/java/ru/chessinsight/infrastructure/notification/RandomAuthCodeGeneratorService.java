package ru.chessinsight.infrastructure.notification;

import org.springframework.stereotype.Service;
import ru.chessinsight.application.auth.service.AuthCodeGeneratorService;

import java.security.SecureRandom;

@Service
public class RandomAuthCodeGeneratorService implements AuthCodeGeneratorService {

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateOtpCode() {
        return generateNumericCode();
    }

    @Override
    public String generateRecoveryCode() {
        return generateNumericCode();
    }

    private String generateNumericCode() {
        int value = secureRandom.nextInt(1_000_000);
        return String.format("%06d", value);
    }
}
