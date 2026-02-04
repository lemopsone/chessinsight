package ru.chessinsight.infrastructure.security.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class RedisRefreshTokenServiceTest {
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisRefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RedisRefreshTokenService(redisTemplate);
    }

    @Test
    void saveToken_deletesExistingToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UUID userId = UUID.randomUUID();
        String userKey = "refresh:user:" + userId;
        when(valueOperations.get(userKey)).thenReturn("old");

        service.saveToken("new", userId);

        verify(redisTemplate).delete("old");
        verify(valueOperations).set(eq("new"), eq(userId.toString()), any(Long.class), eq(TimeUnit.MINUTES));
        verify(valueOperations).set(eq(userKey), eq("new"), any(Long.class), eq(TimeUnit.MINUTES));
    }

    @Test
    void saveToken_skipsDelete_whenNoExistingToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UUID userId = UUID.randomUUID();
        String userKey = "refresh:user:" + userId;
        when(valueOperations.get(userKey)).thenReturn(null);

        service.saveToken("new", userId);

        verify(redisTemplate, never()).delete("old");
        verify(valueOperations).set(eq("new"), eq(userId.toString()), any(Long.class), eq(TimeUnit.MINUTES));
        verify(valueOperations).set(eq(userKey), eq("new"), any(Long.class), eq(TimeUnit.MINUTES));
    }

    @Test
    void tokenValid_returnsTrue_whenKeyExists() {
        when(redisTemplate.hasKey("token")).thenReturn(true);

        assertTrue(service.tokenValid("token"));
    }

    @Test
    void tokenValid_returnsFalse_whenMissing() {
        when(redisTemplate.hasKey("token")).thenReturn(false);

        assertFalse(service.tokenValid("token"));
    }

    @Test
    void getUserIdFromToken_returnsUuid() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UUID userId = UUID.randomUUID();
        when(valueOperations.get("token")).thenReturn(userId.toString());

        UUID result = service.getUserIdFromToken("token");

        assertEquals(userId, result);
    }

    @Test
    void getUserIdFromToken_throws_whenValueMissing() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("token")).thenReturn(null);

        assertThrows(NullPointerException.class, () -> service.getUserIdFromToken("token"));
    }

    @Test
    void invalidateToken_deletesUserKey_whenTokenMatches() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UUID userId = UUID.randomUUID();
        String userKey = "refresh:user:" + userId;
        when(valueOperations.get("token")).thenReturn(userId.toString());
        when(valueOperations.get(userKey)).thenReturn("token");

        service.invalidateToken("token");

        verify(redisTemplate).delete(userKey);
        verify(redisTemplate).delete("token");
    }

    @Test
    void invalidateToken_deletesToken_whenNoUserId() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("token")).thenReturn(null);

        service.invalidateToken("token");

        verify(redisTemplate).delete("token");
        verify(redisTemplate, never()).delete("refresh:user:" + UUID.randomUUID());
    }

    @Test
    void getTokenForUser_returnsToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UUID userId = UUID.randomUUID();
        String userKey = "refresh:user:" + userId;
        when(valueOperations.get(userKey)).thenReturn("token");

        String result = service.getTokenForUser(userId);

        assertEquals("token", result);
    }

    @Test
    void getTokenForUser_returnsNull_whenMissing() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UUID userId = UUID.randomUUID();
        String userKey = "refresh:user:" + userId;
        when(valueOperations.get(userKey)).thenReturn(null);

        String result = service.getTokenForUser(userId);

        assertNull(result);
    }

    @Test
    void invalidateUserTokens_deletesTokenAndUserKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UUID userId = UUID.randomUUID();
        String userKey = "refresh:user:" + userId;
        when(valueOperations.get(userKey)).thenReturn("token");

        service.invalidateUserTokens(userId);

        verify(redisTemplate).delete("token");
        verify(redisTemplate).delete(userKey);
    }

    @Test
    void invalidateUserTokens_deletesUserKey_whenNoToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UUID userId = UUID.randomUUID();
        String userKey = "refresh:user:" + userId;
        when(valueOperations.get(userKey)).thenReturn(null);

        service.invalidateUserTokens(userId);

        verify(redisTemplate, never()).delete("token");
        verify(redisTemplate).delete(userKey);
    }
}