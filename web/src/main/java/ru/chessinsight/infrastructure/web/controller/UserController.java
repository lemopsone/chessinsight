package ru.chessinsight.infrastructure.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.chessinsight.application.statistics.exception.UserNotFoundException;
import ru.chessinsight.application.statistics.service.StatisticsService;
import ru.chessinsight.application.user.service.UserProfileService;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.infrastructure.web.api.UsersApi;
import ru.chessinsight.infrastructure.web.dto.PatchCurrentUserRequest;
import ru.chessinsight.infrastructure.web.dto.UserDTO;
import ru.chessinsight.infrastructure.web.dto.UserStatisticsDTO;
import ru.chessinsight.infrastructure.web.mapper.UserApiMapper;
import ru.chessinsight.application.auth.service.AuthService;
import jakarta.validation.Valid;

@RestController
public class UserController implements UsersApi, ApiV1Controller {

    private final AuthService authService;
    private final StatisticsService statisticsService;
    private final UserApiMapper userApiMapper;
    private final UserProfileService userProfileService;

    public UserController(AuthService authService,
                          StatisticsService statisticsService,
                          UserApiMapper userApiMapper,
                          UserProfileService userProfileService) {
        this.authService = authService;
        this.statisticsService = statisticsService;
        this.userApiMapper = userApiMapper;
        this.userProfileService = userProfileService;
    }

    @Override
    @GetMapping("/users/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        User user = authService.getCurrentUser()
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Not authenticated"));
        return ResponseEntity.ok(userApiMapper.toUserDto(user));
    }

    @Override
    @GetMapping("/users/me/statistics")
    public ResponseEntity<UserStatisticsDTO> getMyStatistics() throws UserNotFoundException {
        var userId = authService.getCurrentUserId()
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Not authenticated"));
        var stats = statisticsService.getUserStatistics(userId);
        return ResponseEntity.ok(userApiMapper.toStatisticsDto(stats));
    }

    @Override
    @PatchMapping("/users/me")
    public ResponseEntity<UserDTO> patchCurrentUser(
            @Valid @RequestBody PatchCurrentUserRequest body
    ) {
        var userId = authService.getCurrentUserId()
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Not authenticated"));
        User user = userProfileService.updateProfile(
                userId,
                body.getEmail(),
                body.getPassword()
        );
        return ResponseEntity.ok(userApiMapper.toUserDto(user));
    }
}
