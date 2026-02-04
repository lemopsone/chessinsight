package ru.chessinsight.infrastructure.security.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class JWTTokenProviderTest {
    private final JWTTokenProvider provider = new JWTTokenProvider("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");

    @Test
    void generateAccessToken_returnsValidToken() {
        String token = provider.generateAccessToken(UUID.randomUUID());

        assertNotNull(token);
        assertTrue(provider.validateToken(token));
    }

    @Test
    void generateAccessToken_differsForDifferentUsers() {
        String token1 = provider.generateAccessToken(UUID.randomUUID());
        String token2 = provider.generateAccessToken(UUID.randomUUID());

        assertNotEquals(token1, token2);
    }

    @Test
    void generateRefreshToken_returnsValidToken() {
        String token = provider.generateRefreshToken(UUID.randomUUID());

        assertNotNull(token);
        assertTrue(provider.validateToken(token));
    }

    @Test
    void generateRefreshToken_differsForDifferentUsers() {
        String token1 = provider.generateRefreshToken(UUID.randomUUID());
        String token2 = provider.generateRefreshToken(UUID.randomUUID());

        assertNotEquals(token1, token2);
    }

    @Test
    void validateToken_returnsFalse_forInvalidToken() {
        assertFalse(provider.validateToken("bad.token.value"));
    }

    @Test
    void validateToken_returnsTrue_forGeneratedToken() {
        String token = provider.generateAccessToken(UUID.randomUUID());

        assertTrue(provider.validateToken(token));
    }

    @Test
    void getUserIdFromToken_returnsUserId() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateAccessToken(userId);

        UUID result = provider.getUserIdFromToken(token);

        assertEquals(userId, result);
    }

    @Test
    void getUserIdFromToken_throws_forInvalidToken() {
        assertThrows(Exception.class, () -> provider.getUserIdFromToken("bad.token.value"));
    }
}