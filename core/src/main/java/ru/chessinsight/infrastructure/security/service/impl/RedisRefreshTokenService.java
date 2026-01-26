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
    private static final String USER_TOKEN_PREFIX = "refresh:user:";

    public RedisRefreshTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void saveToken(String token, UUID userId) {
        String userKey = userKey(userId);
        String existing = redisTemplate.opsForValue().get(userKey);
        if (existing != null) {
            redisTemplate.delete(existing);
        }
        redisTemplate.opsForValue().set(token, userId.toString(), REFRESH_TOKEN_EXPIRATION_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(userKey, token, REFRESH_TOKEN_EXPIRATION_MINUTES, TimeUnit.MINUTES);
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
        String userId = redisTemplate.opsForValue().get(token);
        if (userId != null) {
            String userKey = userKey(UUID.fromString(userId));
            String storedToken = redisTemplate.opsForValue().get(userKey);
            if (token.equals(storedToken)) {
                redisTemplate.delete(userKey);
            }
        }
        redisTemplate.delete(token);
    }

    @Override
    public String getTokenForUser(UUID userId) {
        return redisTemplate.opsForValue().get(userKey(userId));
    }

    @Override
    public void invalidateUserTokens(UUID userId) {
        String token = getTokenForUser(userId);
        if (token != null) {
            redisTemplate.delete(token);
        }
        redisTemplate.delete(userKey(userId));
    }

    private String userKey(UUID userId) {
        return USER_TOKEN_PREFIX + userId;
    }
}
