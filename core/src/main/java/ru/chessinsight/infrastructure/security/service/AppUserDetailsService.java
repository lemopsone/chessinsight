package ru.chessinsight.infrastructure.security.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import ru.chessinsight.application.statistics.exception.UserNotFoundException;
import ru.chessinsight.domain.user.repository.UserRepository;
import ru.chessinsight.infrastructure.security.model.UserPassport;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AppUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final long cacheTtlMs;
    private final Map<UUID, CachedUserDetails> userDetailsCache = new ConcurrentHashMap<>();

    public AppUserDetailsService(
            UserRepository userRepository,
            @Value("${security.auth.user-cache-ttl-seconds:60}") long cacheTtlSeconds
    ) {
        this.userRepository = userRepository;
        this.cacheTtlMs = Math.max(0L, cacheTtlSeconds) * 1000L;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Use loadUserById");
    }

    public UserDetails loadUserById(UUID userId) {
        if (cacheTtlMs <= 0) {
            return fetchUserDetails(userId);
        }

        long now = System.currentTimeMillis();
        CachedUserDetails cached = userDetailsCache.get(userId);
        if (cached != null && cached.validUntilEpochMs() > now) {
            return cached.userDetails();
        }

        UserDetails fresh = fetchUserDetails(userId);
        userDetailsCache.put(userId, new CachedUserDetails(fresh, now + cacheTtlMs));
        return fresh;
    }

    private UserDetails fetchUserDetails(UUID userId) {
        var user = userRepository.findOneById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));
        return new UserPassport(user);
    }

    private record CachedUserDetails(UserDetails userDetails, long validUntilEpochMs) {}
}
