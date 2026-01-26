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
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.infrastructure.web.api.AuthApi;
import ru.chessinsight.infrastructure.web.dto.RefreshTokenRequest;
import ru.chessinsight.infrastructure.web.dto.SignInDTO;
import ru.chessinsight.infrastructure.web.dto.SignUpDTO;
import ru.chessinsight.infrastructure.web.mapper.AuthApiMapper;
import ru.chessinsight.infrastructure.web.mapper.UserApiMapper;

@RestController
public class AuthController implements AuthApi, ApiV1Controller {

    private final AuthService authService;
    private final AuthApiMapper authApiMapper;
    private final UserApiMapper userApiMapper;

    public AuthController(AuthService authService, AuthApiMapper authApiMapper, UserApiMapper userApiMapper) {
        this.authService = authService;
        this.authApiMapper = authApiMapper;
        this.userApiMapper = userApiMapper;
    }

    @Override
    @PostMapping("/auth/signup")
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
    @PostMapping("/auth/signin")
    public ResponseEntity<ru.chessinsight.infrastructure.web.dto.UserTokenDTO> signIn(
            @Valid @RequestBody SignInDTO body
    ) throws WrongCredentialsException, AuthException {
        var appDto = authApiMapper.toAppSignIn(body);
        UserTokenDTO token = authService.signIn(appDto);
        return ResponseEntity.ok(authApiMapper.toApiUserToken(token));
    }

    @Override
    @PostMapping("/auth/refresh")
    public ResponseEntity<ru.chessinsight.infrastructure.web.dto.UserTokenDTO> refreshToken(
            @Valid @RequestBody RefreshTokenRequest body
    ) throws AuthException {
        UserTokenDTO token = authService.refresh(body.getRefreshToken());
        return ResponseEntity.ok(authApiMapper.toApiUserToken(token));
    }

    @Override
    @PostMapping("/auth/signout")
    public ResponseEntity<Void> signOut(@Valid @RequestBody RefreshTokenRequest body) {
        authService.signOut(body.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/auth/me")
    public ResponseEntity<ru.chessinsight.infrastructure.web.dto.UserDTO> getCurrentUserAuth() {
        User user = authService.getCurrentUser()
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Not authenticated"));
        return ResponseEntity.ok(userApiMapper.toUserDto(user));
    }
}
