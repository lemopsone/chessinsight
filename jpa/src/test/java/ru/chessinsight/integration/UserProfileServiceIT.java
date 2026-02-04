package ru.chessinsight.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.chessinsight.application.statistics.exception.UserNotFoundException;
import ru.chessinsight.application.user.service.UserProfileService;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.user.repository.UserRepository;
import ru.chessinsight.infrastructure.security.service.PasswordHasher;

import java.util.UUID;
import ru.chessinsight.testutil.Tag;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
public class UserProfileServiceIT extends AbstractIntegrationTest {

    @Autowired
    private UserProfileService service;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordHasher passwordHasher;

    @Test
    void updateProfile_updatesEmailAndPassword() {
        String login = "user_" + UUID.randomUUID();
        User user = new User();
        user.setLogin(login);
        user.setEmail(login + "@example.com");
        user.setPasswordHash(passwordHasher.hash("oldpass123"));
        user = userRepository.save(user);

        User updated = service.updateProfile(user.getId(), "new_" + login + "@example.com", "newpass123");

        assertEquals("new_" + login + "@example.com", updated.getEmail());
        User fromDb = userRepository.findOneById(user.getId()).orElseThrow();
        assertTrue(passwordHasher.verify("newpass123", fromDb.getPasswordHash()));
    }

    @Test
    void updateProfile_throws_whenMissing() {
        assertThrows(UserNotFoundException.class,
                () -> service.updateProfile(UUID.randomUUID(), "x@example.com", "pass12345"));
    }

    @Test
    void deactivateUser_setsInactive() {
        String login = "user_" + UUID.randomUUID();
        User user = new User();
        user.setLogin(login);
        user.setEmail(login + "@example.com");
        user.setPasswordHash(passwordHasher.hash("pass12345"));
        user = userRepository.save(user);

        service.deactivateUser(user.getId());

        User fromDb = userRepository.findOneById(user.getId()).orElseThrow();
        assertFalse(fromDb.isActive());
    }

    @Test
    void deactivateUser_throws_whenMissing() {
        assertThrows(UserNotFoundException.class, () -> service.deactivateUser(UUID.randomUUID()));
    }
}