package ru.chessinsight.infrastructure.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.chessinsight.application.auth.service.AuthCodeDeliveryService;
import ru.chessinsight.application.common.logger.service.Logger;

@Service
@ConditionalOnProperty(
        prefix = "security.auth.mail",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class NoOpAuthCodeDeliveryService implements AuthCodeDeliveryService {

    private final Logger logger;

    public NoOpAuthCodeDeliveryService(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void sendOtpCode(String email, String code, int ttlSeconds) {
        logger.info("auth.mail.disabled otp_not_sent email=" + email + " ttlSeconds=" + ttlSeconds);
    }

    @Override
    public void sendRecoveryCode(String email, String code, int ttlSeconds) {
        logger.info("auth.mail.disabled recovery_not_sent email=" + email + " ttlSeconds=" + ttlSeconds);
    }
}
