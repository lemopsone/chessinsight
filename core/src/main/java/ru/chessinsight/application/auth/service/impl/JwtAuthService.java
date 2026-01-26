package ru.chessinsight.application.auth.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.chessinsight.application.auth.dto.SignInDTO;
import ru.chessinsight.application.auth.dto.SignUpDTO;
import ru.chessinsight.application.auth.dto.UserTokenDTO;
import ru.chessinsight.application.auth.exception.AuthException;
import ru.chessinsight.application.auth.exception.UserExistsException;
import ru.chessinsight.application.auth.exception.WrongCredentialsException;
import ru.chessinsight.application.auth.service.AuthService;
import ru.chessinsight.application.common.logger.service.Logger;
import ru.chessinsight.domain.user.model.Role;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.user.repository.UserRepository;
import ru.chessinsight.infrastructure.security.service.JWTTokenProvider;
import ru.chessinsight.infrastructure.security.service.PasswordHasher;
import ru.chessinsight.infrastructure.security.service.RefreshTokenService;
import ru.chessinsight.infrastructure.security.model.UserPassport;

import java.util.Optional;
import java.util.UUID;

@Service
public class JwtAuthService implements AuthService {

    private final UserRepository userRepository;
    private final JWTTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final PasswordHasher passwordHasher;
    private final Logger logger;

    public JwtAuthService(UserRepository userRepository,
                          JWTTokenProvider jwtTokenProvider,
                          RefreshTokenService refreshTokenService,
                          PasswordHasher passwordHasher,
                          Logger logger) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.passwordHasher = passwordHasher;
        this.logger = logger;
    }

    @Override
    public UserTokenDTO signUp(SignUpDTO dto) throws UserExistsException {
        Optional<User> existing = userRepository.findOneByLogin(dto.login());
        if (existing.isPresent()) {
            logger.warning("auth.sign_up rejected login=" + dto.login());
            throw new UserExistsException("Login already taken");
        }

        User user = new User();
        user.setLogin(dto.login());
        user.setEmail(dto.email());
        user.setPasswordHash(passwordHasher.hash(dto.password()));
        user.addRole(Role.ROLE_USER);

        user = userRepository.save(user);
        logger.info("auth.sign_up success userId=" + user.getId());
        return generateUserTokens(user.getId());
    }

    @Override
    public UserTokenDTO signIn(SignInDTO dto) throws WrongCredentialsException, AuthException {
        Optional<User> found = userRepository.findOneByLogin(dto.loginOrEmail());
        if (found.isEmpty()) {
            logger.warning("auth.sign_in failed login=" + dto.loginOrEmail());
            throw new WrongCredentialsException("User not found");
        }
        User user = found.get();

        if (!passwordHasher.verify(dto.passwordOrToken(), user.getPasswordHash())) {
            logger.warning("auth.sign_in failed userId=" + user.getId());
            throw new WrongCredentialsException("Wrong password");
        }
        logger.info("auth.sign_in success userId=" + user.getId());
        return generateUserTokens(user.getId());
    }

    @Override
    public UserTokenDTO refresh(String refreshToken) throws AuthException {
        if (!refreshTokenService.tokenValid(refreshToken)) {
            logger.warning("auth.refresh rejected invalidToken");
            throw new AuthException("Refresh token invalid");
        }
        UUID userId = refreshTokenService.getUserIdFromToken(refreshToken);
        logger.info("auth.refresh success userId=" + userId);
        return generateUserTokens(userId);
    }

    @Override
    public void signOut(@Nullable String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.invalidateToken(refreshToken);
            logger.info("auth.sign_out refreshTokenInvalidated");
            return;
        }
        getCurrentUserId().ifPresent(userId -> {
            refreshTokenService.invalidateUserTokens(userId);
            logger.info("auth.sign_out userId=" + userId);
        });
    }

    @Override
    public Optional<UUID> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPassport passport) {
            return Optional.of(UUID.fromString(passport.getId()));
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> getCurrentUser() {
        return getCurrentUserId().flatMap(userRepository::findOneById);
    }

    private UserTokenDTO generateUserTokens(UUID userId) {
        String accessToken = jwtTokenProvider.generateAccessToken(userId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);
        refreshTokenService.saveToken(refreshToken, userId);
        return new UserTokenDTO(accessToken, refreshToken, "Bearer");
    }
}
