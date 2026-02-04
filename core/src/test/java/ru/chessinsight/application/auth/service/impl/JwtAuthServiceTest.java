package ru.chessinsight.application.auth.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.chessinsight.application.auth.dto.AuthType;
import ru.chessinsight.application.auth.dto.SignInDTO;
import ru.chessinsight.application.auth.dto.SignUpDTO;
import ru.chessinsight.application.auth.dto.UserTokenDTO;
import ru.chessinsight.application.auth.exception.AuthException;
import ru.chessinsight.application.auth.exception.UserExistsException;
import ru.chessinsight.application.auth.exception.WrongCredentialsException;
import ru.chessinsight.application.common.logger.service.Logger;
import ru.chessinsight.domain.user.model.Role;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.user.repository.UserRepository;
import ru.chessinsight.infrastructure.security.model.UserPassport;
import ru.chessinsight.infrastructure.security.service.JWTTokenProvider;
import ru.chessinsight.infrastructure.security.service.PasswordHasher;
import ru.chessinsight.infrastructure.security.service.RefreshTokenService;
import ru.chessinsight.testdata.UserBuilder;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import io.qameta.allure.Tag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class JwtAuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private JWTTokenProvider jwtTokenProvider;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private Logger logger;

    private JwtAuthService service;

    @BeforeEach
    void setUp() {
        service = new JwtAuthService(userRepository, jwtTokenProvider, refreshTokenService, passwordHasher, logger);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void signUp_createsUser_andReturnsTokens() {
        SignUpDTO dto = new SignUpDTO("alice", "alice@example.com", "pass");
        when(userRepository.findOneByLogin("alice")).thenReturn(Optional.empty());
        when(passwordHasher.hash("pass")).thenReturn("hashed");
        User saved = UserBuilder.user()
                .withId(UUID.randomUUID())
                .withLogin("alice")
                .withEmail("alice@example.com")
                .withPasswordHash("hashed")
                .withRoles(EnumSet.of(Role.ROLE_USER))
                .build();
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtTokenProvider.generateAccessToken(saved.getId())).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken(saved.getId())).thenReturn("refresh");

        UserTokenDTO result = service.signUp(dto);

        assertEquals("access", result.accessToken());
        assertEquals("refresh", result.refreshToken());
        verify(refreshTokenService).saveToken("refresh", saved.getId());
    }

    @Test
    void signUp_throws_whenLoginTaken() {
        SignUpDTO dto = new SignUpDTO("alice", "alice@example.com", "pass");
        when(userRepository.findOneByLogin("alice")).thenReturn(Optional.of(UserBuilder.user().build()));

        assertThrows(UserExistsException.class, () -> service.signUp(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    void signIn_returnsTokens_whenCredentialsValid() {
        User user = UserBuilder.user()
                .withId(UUID.randomUUID())
                .withLogin("alice")
                .withPasswordHash("hashed")
                .build();
        when(userRepository.findOneByLogin("alice")).thenReturn(Optional.of(user));
        when(passwordHasher.verify("pass", "hashed")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(user.getId())).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken(user.getId())).thenReturn("refresh");

        UserTokenDTO result = service.signIn(new SignInDTO("alice", "pass", AuthType.JWT));

        assertEquals("access", result.accessToken());
        assertEquals("refresh", result.refreshToken());
        verify(refreshTokenService).saveToken("refresh", user.getId());
    }

    @Test
    void signIn_throws_whenUserMissing() {
        when(userRepository.findOneByLogin("missing")).thenReturn(Optional.empty());

        assertThrows(WrongCredentialsException.class, () -> service.signIn(new SignInDTO("missing", "pass", AuthType.JWT)));
    }

    @Test
    void refresh_returnsTokens_whenValid() {
        UUID userId = UUID.randomUUID();
        when(refreshTokenService.tokenValid("refresh")).thenReturn(true);
        when(refreshTokenService.getUserIdFromToken("refresh")).thenReturn(userId);
        when(jwtTokenProvider.generateAccessToken(userId)).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken(userId)).thenReturn("refresh2");

        UserTokenDTO result = service.refresh("refresh");

        assertEquals("access", result.accessToken());
        assertEquals("refresh2", result.refreshToken());
        verify(refreshTokenService).saveToken("refresh2", userId);
    }

    @Test
    void refresh_throws_whenInvalid() {
        when(refreshTokenService.tokenValid("bad")).thenReturn(false);

        assertThrows(AuthException.class, () -> service.refresh("bad"));
    }

    @Test
    void signOut_invalidatesProvidedToken() {
        service.signOut("refresh");

        verify(refreshTokenService).invalidateToken("refresh");
        verify(refreshTokenService, never()).invalidateUserTokens(any());
    }

    @Test
    void signOut_invalidatesCurrentUserTokens_whenNoTokenProvided() {
        UUID userId = UUID.randomUUID();
        User user = UserBuilder.user().withId(userId).build();
        UserPassport passport = new UserPassport(user);
        var auth = new UsernamePasswordAuthenticationToken(passport, null, passport.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        service.signOut(null);

        verify(refreshTokenService).invalidateUserTokens(userId);
    }

    @Test
    void getCurrentUserId_returnsId_whenAuthenticated() {
        UUID userId = UUID.randomUUID();
        User user = UserBuilder.user().withId(userId).build();
        UserPassport passport = new UserPassport(user);
        var auth = new UsernamePasswordAuthenticationToken(passport, null, passport.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<UUID> result = service.getCurrentUserId();

        assertTrue(result.isPresent());
        assertEquals(userId, result.get());
    }

    @Test
    void getCurrentUserId_returnsEmpty_whenUnauthenticated() {
        SecurityContextHolder.getContext().setAuthentication(null);

        Optional<UUID> result = service.getCurrentUserId();

        assertTrue(result.isEmpty());
    }

    @Test
    void getCurrentUser_returnsUser_whenAuthenticated() {
        UUID userId = UUID.randomUUID();
        User user = UserBuilder.user().withId(userId).build();
        UserPassport passport = new UserPassport(user);
        var auth = new UsernamePasswordAuthenticationToken(passport, null, passport.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(userRepository.findOneById(userId)).thenReturn(Optional.of(user));

        Optional<User> result = service.getCurrentUser();

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void getCurrentUser_returnsEmpty_whenNoUser() {
        SecurityContextHolder.getContext().setAuthentication(null);

        Optional<User> result = service.getCurrentUser();

        assertTrue(result.isEmpty());
    }
}