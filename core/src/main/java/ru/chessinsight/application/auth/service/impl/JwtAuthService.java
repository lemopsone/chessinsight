package ru.chessinsight.application.auth.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.chessinsight.application.auth.dto.AuthType;
import ru.chessinsight.application.auth.dto.RecoveryConfirmDTO;
import ru.chessinsight.application.auth.dto.RecoveryRequestDTO;
import ru.chessinsight.application.auth.dto.SignInDTO;
import ru.chessinsight.application.auth.dto.SignUpDTO;
import ru.chessinsight.application.auth.dto.UserTokenDTO;
import ru.chessinsight.application.auth.exception.AccountLockedException;
import ru.chessinsight.application.auth.exception.AuthException;
import ru.chessinsight.application.auth.exception.UserExistsException;
import ru.chessinsight.application.auth.exception.WrongCredentialsException;
import ru.chessinsight.application.auth.service.AuthCodeDeliveryService;
import ru.chessinsight.application.auth.service.AuthCodeGeneratorService;
import ru.chessinsight.application.auth.service.AuthService;
import ru.chessinsight.application.common.logger.service.Logger;
import ru.chessinsight.domain.user.model.Role;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.user.repository.UserRepository;
import ru.chessinsight.infrastructure.security.model.UserPassport;
import ru.chessinsight.infrastructure.security.service.JWTTokenProvider;
import ru.chessinsight.infrastructure.security.service.PasswordHasher;
import ru.chessinsight.infrastructure.security.service.RefreshTokenService;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JwtAuthService implements AuthService {

    private static final int DEFAULT_OTP_TTL_SECONDS = 300;
    private static final int DEFAULT_RECOVERY_TTL_SECONDS = 600;

    private final UserRepository userRepository;
    private final JWTTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final PasswordHasher passwordHasher;
    private final Logger logger;
    private final AuthCodeDeliveryService authCodeDeliveryService;
    private final AuthCodeGeneratorService authCodeGeneratorService;
    private final boolean emailOtpRequired;
    private final int maxFailedAttempts;
    private final int otpTtlSeconds;
    private final int recoveryTtlSeconds;

    private final ConcurrentHashMap<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AuthCodeChallenge> otpChallenges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AuthCodeChallenge> recoveryChallenges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> lockedAccounts = new ConcurrentHashMap<>();

    public JwtAuthService(UserRepository userRepository,
                          JWTTokenProvider jwtTokenProvider,
                          RefreshTokenService refreshTokenService,
                          PasswordHasher passwordHasher,
                          Logger logger,
                          AuthCodeDeliveryService authCodeDeliveryService,
                          AuthCodeGeneratorService authCodeGeneratorService,
                          @Value("${security.auth.require-email-otp:false}") boolean emailOtpRequired,
                          @Value("${security.auth.max-failed-attempts:5}") int maxFailedAttempts,
                          @Value("${security.auth.otp.ttl-seconds:" + DEFAULT_OTP_TTL_SECONDS + "}") int otpTtlSeconds,
                          @Value("${security.auth.recovery.ttl-seconds:" + DEFAULT_RECOVERY_TTL_SECONDS + "}") int recoveryTtlSeconds) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.passwordHasher = passwordHasher;
        this.logger = logger;
        this.authCodeDeliveryService = authCodeDeliveryService;
        this.authCodeGeneratorService = authCodeGeneratorService;
        this.emailOtpRequired = emailOtpRequired;
        this.maxFailedAttempts = Math.max(1, maxFailedAttempts);
        this.otpTtlSeconds = otpTtlSeconds > 0 ? otpTtlSeconds : DEFAULT_OTP_TTL_SECONDS;
        this.recoveryTtlSeconds = recoveryTtlSeconds > 0 ? recoveryTtlSeconds : DEFAULT_RECOVERY_TTL_SECONDS;
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
        User user = findUser(dto.loginOrEmail())
                .orElseThrow(() -> {
                    logger.warning("auth.sign_in failed loginOrEmail=" + dto.loginOrEmail());
                    return new WrongCredentialsException("Wrong credentials");
                });

        String key = accountKey(user);
        ensureNotLocked(key, user.getId());

        if (!user.isActive()) {
            logger.warning("auth.sign_in rejected inactive userId=" + user.getId());
            throw new AuthException("Account is inactive");
        }

        if (dto.authType() == AuthType.EMAIL_OTP) {
            return completeEmailOtpSignIn(user, key, dto.passwordOrToken());
        }

        if (!passwordHasher.verify(dto.passwordOrToken(), user.getPasswordHash())) {
            registerFailureAndThrow(user, key);
        }

        resetAuthFailures(key);

        if (!emailOtpRequired) {
            logger.info("auth.sign_in success userId=" + user.getId());
            return generateUserTokens(user.getId());
        }

        issueOtpChallenge(key, user.getLogin(), user.getEmail(), otpTtlSeconds);
        throw new AuthException("Second factor required: check your email");
    }

    @Override
    public void requestAccountRecovery(RecoveryRequestDTO dto) {
        Optional<User> userOpt = findUser(dto.loginOrEmail());
        if (userOpt.isEmpty()) {
            logger.info("auth.recovery.request ignored unknown loginOrEmail=" + dto.loginOrEmail());
            return;
        }

        User user = userOpt.get();
        String key = accountKey(user);
        if (!isLocked(key)) {
            logger.info("auth.recovery.request ignored unlocked userId=" + user.getId());
            return;
        }

        String code = authCodeGeneratorService.generateRecoveryCode();
        recoveryChallenges.put(key, new AuthCodeChallenge(code, Instant.now().plusSeconds(recoveryTtlSeconds)));
        authCodeDeliveryService.sendRecoveryCode(user.getEmail(), code, recoveryTtlSeconds);
        logger.info("auth.recovery.code.sent userId=" + user.getId() + " channel=email");
    }

    @Override
    public void confirmAccountRecovery(RecoveryConfirmDTO dto) throws AuthException {
        User user = findUser(dto.loginOrEmail())
                .orElseThrow(() -> new AuthException("Recovery request is invalid"));
        String key = accountKey(user);

        AuthCodeChallenge challenge = recoveryChallenges.get(key);
        if (challenge == null || challenge.expireAt().isBefore(Instant.now())) {
            recoveryChallenges.remove(key);
            throw new AuthException("Recovery code expired or missing");
        }

        if (!challenge.code().equals(dto.recoveryCode())) {
            throw new AuthException("Recovery code is invalid");
        }

        user.setPasswordHash(passwordHasher.hash(dto.newPassword()));
        userRepository.save(user);

        recoveryChallenges.remove(key);
        otpChallenges.remove(key);
        resetAuthFailures(key);
        refreshTokenService.invalidateUserTokens(user.getId());

        logger.info("auth.recovery.confirmed userId=" + user.getId());
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

    private UserTokenDTO completeEmailOtpSignIn(User user, String key, String inputCode) {
        if (!emailOtpRequired) {
            throw new AuthException("Email OTP is disabled");
        }

        AuthCodeChallenge challenge = otpChallenges.get(key);
        if (challenge == null || challenge.expireAt().isBefore(Instant.now())) {
            otpChallenges.remove(key);
            throw new AuthException("Second factor code expired or missing");
        }

        if (!challenge.code().equals(inputCode)) {
            registerFailureAndThrow(user, key);
        }

        otpChallenges.remove(key);
        resetAuthFailures(key);
        logger.info("auth.sign_in success userId=" + user.getId() + " method=email_otp");
        return generateUserTokens(user.getId());
    }

    private Optional<User> findUser(String loginOrEmail) {
        Optional<User> byLogin = userRepository.findOneByLogin(loginOrEmail);
        if (byLogin.isPresent()) {
            return byLogin;
        }
        return userRepository.findOneByEmail(loginOrEmail);
    }

    private void issueOtpChallenge(String key, String login, String email, int ttlSeconds) {
        String code = authCodeGeneratorService.generateOtpCode();
        otpChallenges.put(key, new AuthCodeChallenge(code, Instant.now().plusSeconds(ttlSeconds)));
        authCodeDeliveryService.sendOtpCode(email, code, ttlSeconds);
        logger.info("auth.email_otp.sent login=" + login + " email=" + email);
    }

    private String accountKey(User user) {
        return user.getId().toString();
    }

    private void ensureNotLocked(String key, UUID userId) {
        if (isLocked(key)) {
            logger.warning("auth.sign_in locked userId=" + userId);
            throw new AccountLockedException("Account is locked due to failed attempts");
        }
    }

    private boolean isLocked(String key) {
        return lockedAccounts.getOrDefault(key, false);
    }

    private void registerFailureAndThrow(User user, String key) {
        int attempts = failedAttempts.merge(key, 1, Integer::sum);
        if (attempts >= maxFailedAttempts) {
            lockedAccounts.put(key, true);
            logger.warning("auth.sign_in locked userId=" + user.getId() + " attempts=" + attempts);
            throw new AccountLockedException("Account is locked due to failed attempts");
        }

        logger.warning("auth.sign_in failed userId=" + user.getId() + " attempts=" + attempts);
        throw new WrongCredentialsException("Wrong credentials");
    }

    private void resetAuthFailures(String key) {
        failedAttempts.remove(key);
        lockedAccounts.remove(key);
    }

    private UserTokenDTO generateUserTokens(UUID userId) {
        String accessToken = jwtTokenProvider.generateAccessToken(userId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);
        refreshTokenService.saveToken(refreshToken, userId);
        return new UserTokenDTO(accessToken, refreshToken, "Bearer");
    }

    private record AuthCodeChallenge(String code, Instant expireAt) {}
}
