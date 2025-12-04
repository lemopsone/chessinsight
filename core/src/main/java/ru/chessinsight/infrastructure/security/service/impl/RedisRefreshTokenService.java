package ru.chessinsight.infrastructure.security.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import ru.chessinsight.infrastructure.security.service.RefreshTokenService;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RedisRefreshTokenService implements RefreshTokenService {
    private final StringRedisTemplate redisTemplate;

    private static final long REFRESH_TOKEN_EXPIRATION_MINUTES = 7 * 24 * 60;

    public RedisRefreshTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void saveToken(String token, UUID userId) {
        redisTemplate.opsForValue().set(token, userId.toString(), REFRESH_TOKEN_EXPIRATION_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public boolean tokenValid(String token) {
        return redisTemplate.hasKey(token);
    }

    @Override
    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(redisTemplate.opsForValue().get(token));
    }

    @Override
    public void invalidateToken(String token) {
        redisTemplate.delete(token);
    }
}
