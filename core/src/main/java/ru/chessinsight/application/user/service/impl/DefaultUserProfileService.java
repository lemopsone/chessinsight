package ru.chessinsight.application.user.service.impl;

import org.springframework.stereotype.Service;
import ru.chessinsight.application.common.logger.service.Logger;
import ru.chessinsight.application.statistics.exception.UserNotFoundException;
import ru.chessinsight.application.user.service.UserProfileService;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.user.repository.UserRepository;
import ru.chessinsight.infrastructure.security.service.PasswordHasher;

import java.util.UUID;

@Service
public class DefaultUserProfileService implements UserProfileService {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final Logger logger;

    public DefaultUserProfileService(UserRepository userRepository,
                                     PasswordHasher passwordHasher,
                                     Logger logger) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.logger = logger;
    }

    @Override
    public User updateProfile(UUID userId, String email, String password) {
        User user = userRepository.findOneById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        boolean emailChanged = false;
        boolean passwordChanged = false;

        if (email != null) {
            user.setEmail(email);
            emailChanged = true;
        }
        if (password != null) {
            user.setPasswordHash(passwordHasher.hash(password));
            passwordChanged = true;
        }

        if (emailChanged || passwordChanged) {
            user = userRepository.save(user);
        }

        logger.info("user.profile.updated userId=" + userId
                + " emailChanged=" + emailChanged
                + " passwordChanged=" + passwordChanged);
        return user;
    }
}
