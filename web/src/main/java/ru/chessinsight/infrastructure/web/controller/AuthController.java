package ru.chessinsight.infrastructure.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import ru.chessinsight.application.auth.dto.UserTokenDTO;
import ru.chessinsight.application.auth.exception.AuthException;
import ru.chessinsight.application.auth.exception.UserExistsException;
import ru.chessinsight.application.auth.exception.WrongCredentialsException;
import ru.chessinsight.application.auth.service.AuthService;
import ru.chessinsight.infrastructure.web.api.AuthApi;
import ru.chessinsight.infrastructure.web.dto.SignInDTO;
import ru.chessinsight.infrastructure.web.dto.RecoveryConfirmDTO;
import ru.chessinsight.infrastructure.web.dto.RecoveryRequestDTO;
import ru.chessinsight.infrastructure.web.dto.SignOutRequest;
import ru.chessinsight.infrastructure.web.dto.SignUpDTO;
import ru.chessinsight.infrastructure.web.mapper.AuthApiMapper;

@RestController
public class AuthController implements AuthApi, ApiV1Controller {

    private final AuthService authService;
    private final AuthApiMapper authApiMapper;

    public AuthController(AuthService authService, AuthApiMapper authApiMapper) {
        this.authService = authService;
        this.authApiMapper = authApiMapper;
    }

    @Override
    @PostMapping("/users")
    public ResponseEntity<ru.chessinsight.infrastructure.web.dto.UserTokenDTO> signUp(
            @Valid @RequestBody SignUpDTO body
    ) throws UserExistsException {
        var appDto = authApiMapper.toAppSignUp(body);
        UserTokenDTO token = authService.signUp(appDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authApiMapper.toApiUserToken(token));
    }

    @Override
    @PostMapping("/auth/sessions")
    public ResponseEntity<ru.chessinsight.infrastructure.web.dto.UserTokenDTO> signIn(
            @Valid @RequestBody SignInDTO body
    ) throws WrongCredentialsException, AuthException {
        var appDto = authApiMapper.toAppSignIn(body);
        UserTokenDTO token = authService.signIn(appDto);
        return ResponseEntity.ok(authApiMapper.toApiUserToken(token));
    }

    @Override
    @PostMapping("/auth/tokens")
    public ResponseEntity<ru.chessinsight.infrastructure.web.dto.UserTokenDTO> refreshToken(
            @Valid @RequestBody SignOutRequest body
    ) throws AuthException {
        UserTokenDTO token = authService.refresh(body.getRefreshToken());
        return ResponseEntity.ok(authApiMapper.toApiUserToken(token));
    }

    @Override
    @DeleteMapping("/auth/sessions")
    public ResponseEntity<Void> signOut(@Valid @RequestBody SignOutRequest body) {
        authService.signOut(body.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/auth/recovery/request")
    public ResponseEntity<Void> requestRecovery(@Valid @RequestBody RecoveryRequestDTO body) {
        authService.requestAccountRecovery(new ru.chessinsight.application.auth.dto.RecoveryRequestDTO(
                body.getLoginOrEmail()
        ));
        return ResponseEntity.accepted().build();
    }

    @Override
    @PostMapping("/auth/recovery/confirm")
    public ResponseEntity<Void> confirmRecovery(@Valid @RequestBody RecoveryConfirmDTO body) {
        authService.confirmAccountRecovery(new ru.chessinsight.application.auth.dto.RecoveryConfirmDTO(
                body.getLoginOrEmail(),
                body.getRecoveryCode(),
                body.getNewPassword()
        ));
        return ResponseEntity.noContent().build();
    }
}
