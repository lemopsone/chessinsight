package ru.chessinsight.infrastructure.security.service.impl;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import ru.chessinsight.infrastructure.security.service.PasswordHasher;

@Component
public class BCryptPasswordHasher implements PasswordHasher {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hash(String raw) {
        return encoder.encode(raw);
    }

    public boolean verify(String raw, String hash) {
        return encoder.matches(raw, hash);
    }
}
