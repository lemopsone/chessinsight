package ru.chessinsight.application.auth.service.impl;

import org.springframework.stereotype.Service;
import ru.chessinsight.application.auth.dto.SignInDTO;
import ru.chessinsight.application.auth.dto.SignUpDTO;
import ru.chessinsight.application.auth.dto.UserTokenDTO;
import ru.chessinsight.application.auth.exception.AuthException;
import ru.chessinsight.application.auth.exception.UserExistsException;
import ru.chessinsight.application.auth.exception.WrongCredentialsException;
import ru.chessinsight.application.auth.service.AuthService;
import ru.chessinsight.domain.user.model.Role;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.user.repository.UserRepository;
import ru.chessinsight.infrastructure.security.service.JWTTokenProvider;
import ru.chessinsight.infrastructure.security.service.PasswordHasher;
import ru.chessinsight.infrastructure.security.service.RefreshTokenService;

import java.util.Optional;
import java.util.UUID;

@Service
public class JwtAuthService implements AuthService {

    private final UserRepository userRepository;
    private final JWTTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final PasswordHasher passwordHasher;

    private UUID currentUserId;

    public JwtAuthService(UserRepository userRepository,
                          JWTTokenProvider jwtTokenProvider,
                          RefreshTokenService refreshTokenService, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public UserTokenDTO signUp(SignUpDTO dto) throws UserExistsException {
        Optional<User> existing = userRepository.findOneByLogin(dto.login());
        if (existing.isPresent()) {
            throw new UserExistsException("Login already taken");
        }

        User user = new User();
        user.setLogin(dto.login());
        user.setEmail(dto.email());
        user.setPasswordHash(passwordHasher.hash(dto.password()));
        user.addRole(Role.ROLE_USER);

        user = userRepository.save(user);

        this.currentUserId = user.getId();
        return generateUserTokens(user.getId());
    }

    @Override
    public UserTokenDTO signIn(SignInDTO dto) throws WrongCredentialsException, AuthException {
        User user = userRepository.findOneByLogin(dto.loginOrEmail())
                .orElseThrow(() -> new WrongCredentialsException("User not found"));

        if (!passwordHasher.verify(dto.passwordOrToken(), user.getPasswordHash())) {
            throw new WrongCredentialsException("Wrong password");
        }

        this.currentUserId = user.getId();
        return generateUserTokens(user.getId());
    }

    @Override
    public UserTokenDTO refresh(String refreshToken) throws AuthException {
        if (!refreshTokenService.tokenValid(refreshToken)) {
            throw new AuthException("Refresh token invalid");
        }
        UUID userId = refreshTokenService.getUserIdFromToken(refreshToken);
        this.currentUserId = userId;
        return generateUserTokens(userId);
    }

    @Override
    public void signOut(String refreshToken) {
        refreshTokenService.invalidateToken(refreshToken);
        this.currentUserId = null;
    }

    @Override
    public Optional<UUID> getCurrentUserId() {
        return Optional.ofNullable(currentUserId);
    }

    @Override
    public Optional<User> getCurrentUser() {
        if (currentUserId == null) return Optional.empty();
        return userRepository.findOneById(currentUserId);
    }

    private UserTokenDTO generateUserTokens(UUID userId) {
        String accessToken = jwtTokenProvider.generateAccessToken(userId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);
        refreshTokenService.saveToken(refreshToken, userId);
        return new UserTokenDTO(accessToken, refreshToken, "Bearer");
    }
}
