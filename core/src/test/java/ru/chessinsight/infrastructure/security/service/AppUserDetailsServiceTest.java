package ru.chessinsight.infrastructure.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import ru.chessinsight.application.statistics.exception.UserNotFoundException;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.user.repository.UserRepository;
import ru.chessinsight.infrastructure.security.model.UserPassport;
import ru.chessinsight.testdata.UserBuilder;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {
    @Mock
    private UserRepository userRepository;

    private AppUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new AppUserDetailsService(userRepository);
    }

    @Test
    void loadUserByUsername_throwsUnsupported() {
        assertThrows(UnsupportedOperationException.class, () -> service.loadUserByUsername("user"));
    }

    @Test
    void loadUserByUsername_throwsUnsupported_whenEmpty() {
        assertThrows(UnsupportedOperationException.class, () -> service.loadUserByUsername(""));
    }

    @Test
    void loadUserById_returnsPassport() {
        UUID userId = UUID.randomUUID();
        User user = UserBuilder.user().withId(userId).build();
        when(userRepository.findOneById(userId)).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserById(userId);

        assertTrue(details instanceof UserPassport);
        assertEquals(user.getLogin(), details.getUsername());
    }

    @Test
    void loadUserById_throws_whenMissing() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findOneById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.loadUserById(userId));
    }
}