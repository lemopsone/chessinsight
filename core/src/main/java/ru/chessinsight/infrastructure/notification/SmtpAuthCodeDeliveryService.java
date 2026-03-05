package ru.chessinsight.infrastructure.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import ru.chessinsight.application.auth.exception.AuthException;
import ru.chessinsight.application.auth.service.AuthCodeDeliveryService;
import ru.chessinsight.application.common.logger.service.Logger;

@Service
@ConditionalOnProperty(
        prefix = "security.auth.mail",
        name = "enabled",
        havingValue = "true"
)
public class SmtpAuthCodeDeliveryService implements AuthCodeDeliveryService {

    private final JavaMailSender mailSender;
    private final Logger logger;
    private final String fromAddress;

    public SmtpAuthCodeDeliveryService(JavaMailSender mailSender,
                                       Logger logger,
                                       @Value("${security.auth.mail.from:${spring.mail.username:no-reply@chessinsight.local}}")
                                       String fromAddress) {
        this.mailSender = mailSender;
        this.logger = logger;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendOtpCode(String email, String code, int ttlSeconds) {
        String subject = "ChessInsight: verification code";
        String body = "Your verification code is: " + code + "\n"
                + "It expires in " + ttlSeconds + " seconds.\n"
                + "If this wasn't you, ignore this message.";
        send(email, subject, body, "otp");
    }

    @Override
    public void sendRecoveryCode(String email, String code, int ttlSeconds) {
        String subject = "ChessInsight: account recovery code";
        String body = "Your account recovery code is: " + code + "\n"
                + "It expires in " + ttlSeconds + " seconds.\n"
                + "If this wasn't you, ignore this message.";
        send(email, subject, body, "recovery");
    }

    private void send(String recipient, String subject, String body, String kind) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
            logger.info("auth.mail.sent kind=" + kind + " email=" + recipient);
        } catch (MailException ex) {
            logger.error("auth.mail.failed kind=" + kind + " email=" + recipient + " reason=" + ex.getMessage());
            throw new AuthException("Failed to send authentication email");
        }
    }
}
