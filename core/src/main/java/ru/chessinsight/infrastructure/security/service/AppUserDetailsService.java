package ru.chessinsight.infrastructure.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import ru.chessinsight.application.statistics.exception.UserNotFoundException;
import ru.chessinsight.domain.user.repository.UserRepository;
import ru.chessinsight.infrastructure.security.model.UserPassport;

import java.util.UUID;

@Service
public class AppUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Use loadUserById");
    }

    public UserDetails loadUserById(UUID userId) {
        var user = userRepository.findOneById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        return new UserPassport(user);
    }
}
