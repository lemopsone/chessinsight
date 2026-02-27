package ru.chessinsight.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.chessinsight.infrastructure.security.service.RefreshTokenService;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@TestConfiguration(proxyBeanMethods = false)
public class WebContainerTestConfig {

    @Bean
    @ServiceConnection
    @ConditionalOnProperty(name = "test.db.mode", havingValue = "testcontainers", matchIfMissing = true)
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
                .withInitScript("schema.sql");
    }

    @Bean
    @Primary
    RefreshTokenService refreshTokenService() {
        return new InMemoryRefreshTokenService();
    }

    static class InMemoryRefreshTokenService implements RefreshTokenService {
        private final ConcurrentHashMap<String, UUID> tokenToUser = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<UUID, String> userToToken = new ConcurrentHashMap<>();

        @Override
        public void saveToken(String token, UUID userId) {
            String existing = userToToken.put(userId, token);
            if (existing != null) {
                tokenToUser.remove(existing);
            }
            tokenToUser.put(token, userId);
        }

        @Override
        public boolean tokenValid(String token) {
            return tokenToUser.containsKey(token);
        }

        @Override
        public UUID getUserIdFromToken(String token) {
            return tokenToUser.get(token);
        }

        @Override
        public void invalidateToken(String token) {
            UUID userId = tokenToUser.remove(token);
            if (userId != null) {
                userToToken.remove(userId);
            }
        }

        @Override
        public String getTokenForUser(UUID userId) {
            return userToToken.get(userId);
        }

        @Override
        public void invalidateUserTokens(UUID userId) {
            String token = userToToken.remove(userId);
            if (token != null) {
                tokenToUser.remove(token);
            }
        }
    }
}
