package ru.chessinsight.infrastructure.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.chessinsight.application.statistics.exception.UserNotFoundException;
import ru.chessinsight.application.statistics.service.StatisticsService;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.infrastructure.web.api.UsersApi;
import ru.chessinsight.infrastructure.web.dto.UserDTO;
import ru.chessinsight.infrastructure.web.dto.UserStatisticsDTO;
import ru.chessinsight.infrastructure.web.mapper.UserApiMapper;
import ru.chessinsight.application.auth.service.AuthService;

@RestController
public class UserController implements UsersApi, ApiV1Controller {

    private final AuthService authService;
    private final StatisticsService statisticsService;
    private final UserApiMapper userApiMapper;

    public UserController(AuthService authService, StatisticsService statisticsService, UserApiMapper userApiMapper) {
        this.authService = authService;
        this.statisticsService = statisticsService;
        this.userApiMapper = userApiMapper;
    }

    @GetMapping("/users/me")
    public ResponseEntity<UserDTO> currentUser() {
        User user = authService.getCurrentUser()
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Not authenticated"));
        return ResponseEntity.ok(userApiMapper.toUserDto(user));
    }

    @GetMapping("/users/me/statistics")
    public ResponseEntity<UserStatisticsDTO> myStatistics() throws UserNotFoundException {
        var userId = authService.getCurrentUserId()
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Not authenticated"));
        var stats = statisticsService.getUserStatistics(userId);
        return ResponseEntity.ok(userApiMapper.toStatisticsDto(stats));
    }
}
