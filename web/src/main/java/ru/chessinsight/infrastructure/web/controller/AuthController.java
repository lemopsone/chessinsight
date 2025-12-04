package ru.chessinsight.infrastructure.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.chessinsight.application.auth.dto.UserTokenDTO;
import ru.chessinsight.application.auth.exception.AuthException;
import ru.chessinsight.application.auth.exception.UserExistsException;
import ru.chessinsight.application.auth.exception.WrongCredentialsException;
import ru.chessinsight.application.auth.service.AuthService;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.infrastructure.web.api.AuthApi;
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

    @PostMapping("/auth/signup")
    public ResponseEntity<ru.chessinsight.infrastructure.web.dto.UserTokenDTO> signUp(
            @RequestBody SignUpDTO body
    ) throws UserExistsException {
        var appDto = authApiMapper.toAppSignUp(body);
        UserTokenDTO token = authService.signUp(appDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authApiMapper.toApiUserToken(token));
    }

    @PostMapping("/auth/signin")
    public ResponseEntity<ru.chessinsight.infrastructure.web.dto.UserTokenDTO> signIn(
            @RequestBody SignInDTO body
    ) throws WrongCredentialsException, AuthException {
        var appDto = authApiMapper.toAppSignIn(body);
        UserTokenDTO token = authService.signIn(appDto);
        return ResponseEntity.ok(authApiMapper.toApiUserToken(token));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<ru.chessinsight.infrastructure.web.dto.UserTokenDTO> refresh(
            @RequestBody java.util.Map<String, String> body
    ) throws AuthException {
        String refreshToken = body.get("refreshToken");
        UserTokenDTO token = authService.refresh(refreshToken);
        return ResponseEntity.ok(authApiMapper.toApiUserToken(token));
    }

    @PostMapping("/auth/signout")
    public ResponseEntity<Void> signOut(@RequestBody(required = false) java.util.Map<String, String> body) {
        if (body != null) {
            String refreshToken = body.get("refreshToken");
            if (refreshToken != null) {
                authService.signOut(refreshToken);
            }
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/auth/me")
    public ResponseEntity<ru.chessinsight.infrastructure.web.dto.UserDTO> meViaAuth() {
        User user = authService.getCurrentUser()
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Not authenticated"));
        return ResponseEntity.ok(userApiMapper.toUserDto(user));
    }
}
