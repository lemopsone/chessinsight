package ru.chessinsight.infrastructure.web.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.chessinsight.application.admin.service.AdminUserService;
import ru.chessinsight.application.auth.service.AuthService;
import ru.chessinsight.application.statistics.exception.UserNotFoundException;
import ru.chessinsight.application.statistics.service.StatisticsService;
import ru.chessinsight.application.user.service.UserProfileService;
import ru.chessinsight.domain.common.pagination.PageParams;
import ru.chessinsight.domain.user.model.Role;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.infrastructure.web.api.UsersApi;
import ru.chessinsight.infrastructure.web.dto.AdminPatchUserRequest;
import ru.chessinsight.infrastructure.web.dto.AdminUserDTO;
import ru.chessinsight.infrastructure.web.dto.PageResponseAdminUserDTO;
import ru.chessinsight.infrastructure.web.dto.PatchCurrentUserRequest;
import ru.chessinsight.infrastructure.web.dto.UserDTO;
import ru.chessinsight.infrastructure.web.dto.UserRole;
import ru.chessinsight.infrastructure.web.dto.UserStatisticsDTO;
import ru.chessinsight.infrastructure.web.mapper.UserApiMapper;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
public class UserController implements UsersApi, ApiV1Controller {

    private final AuthService authService;
    private final StatisticsService statisticsService;
    private final UserApiMapper userApiMapper;
    private final UserProfileService userProfileService;
    private final AdminUserService adminUserService;

    public UserController(AuthService authService,
                          StatisticsService statisticsService,
                          UserApiMapper userApiMapper,
                          UserProfileService userProfileService,
                          AdminUserService adminUserService) {
        this.authService = authService;
        this.statisticsService = statisticsService;
        this.userApiMapper = userApiMapper;
        this.userProfileService = userProfileService;
        this.adminUserService = adminUserService;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<PageResponseAdminUserDTO> listUsersAdmin(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) List<UserRole> roles
    ) {
        int p = page != null ? page : 0;
        int s = size != null ? size : 20;

        Set<Role> roleFilter = userApiMapper.toDomainRoles(roles);
        var resultPage = adminUserService.listUsers(active, roleFilter, new PageParams(p, s));
        return ResponseEntity.ok(userApiMapper.toAdminUserPageResponse(resultPage));
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

    @Override
    @DeleteMapping("/user/me")
    public ResponseEntity<Void> deleteCurrentUser() {
        var userId = authService.getCurrentUserId()
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Not authenticated"));
        userProfileService.deactivateUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<AdminUserDTO> adminGetUser(@PathVariable UUID userId) {
        User user = adminUserService.getUser(userId);
        return ResponseEntity.ok(userApiMapper.toAdminUserDto(user));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/user/{userId}")
    public ResponseEntity<AdminUserDTO> adminPatchUser(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminPatchUserRequest patch
    ) {
        Set<Role> roles = patch.getRoles() == null ? null :
                userApiMapper.toDomainRoles(patch.getRoles());

        User user = adminUserService.patchUser(
                userId,
                patch.getLogin(),
                patch.getEmail(),
                roles,
                patch.getActive()
        );
        return ResponseEntity.ok(userApiMapper.toAdminUserDto(user));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> adminDeactivateUser(@PathVariable UUID userId) {
        adminUserService.deactivateUser(userId);
        return ResponseEntity.noContent().build();
    }
}
