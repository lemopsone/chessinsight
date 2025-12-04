package ru.chessinsight.infrastructure.security.service;

import java.util.UUID;

public interface RefreshTokenService {
    void saveToken(String token, UUID userId);
    boolean tokenValid(String token);
    UUID getUserIdFromToken(String token);
    void invalidateToken(String token);
}
