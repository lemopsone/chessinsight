package ru.chessinsight.application.user.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.chessinsight.application.common.logger.service.Logger;
import ru.chessinsight.application.statistics.exception.UserNotFoundException;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.user.repository.UserRepository;
import ru.chessinsight.infrastructure.security.service.PasswordHasher;
import ru.chessinsight.testdata.UserBuilder;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DefaultUserProfileServiceLondonTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private Logger logger;
    @Captor
    private ArgumentCaptor<User> userCaptor;

    private DefaultUserProfileService service;

    @BeforeEach
    void setUp() {
        service = new DefaultUserProfileService(userRepository, passwordHasher, logger);
    }

    @Test
    void updateProfile_updatesEmailAndPassword_whenProvided() {
        UUID userId = UUID.randomUUID();
        User existing = UserBuilder.user()
                .withId(userId)
                .withEmail("old@example.com")
                .withPasswordHash("old")
                .build();
        when(userRepository.findOneById(userId)).thenReturn(Optional.of(existing));
        when(passwordHasher.hash("newpass")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.updateProfile(userId, "new@example.com", "newpass");

        assertEquals("new@example.com", result.getEmail());
        assertEquals("hashed", result.getPasswordHash());
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("new@example.com", saved.getEmail());
        assertEquals("hashed", saved.getPasswordHash());
    }

    @Test
    void updateProfile_throws_whenUserMissing() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findOneById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> service.updateProfile(userId, "x@example.com", "pass"));
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordHasher);
    }

    @Test
    void deactivateUser_setsInactive_andPersists() {
        UUID userId = UUID.randomUUID();
        User existing = UserBuilder.user()
                .withId(userId)
                .withActive(true)
                .build();
        when(userRepository.findOneById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deactivateUser(userId);

        assertFalse(existing.isActive());
        verify(userRepository).save(existing);
    }

    @Test
    void deactivateUser_throws_whenUserMissing() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findOneById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.deactivateUser(userId));
        verify(userRepository, never()).save(any());
    }
}