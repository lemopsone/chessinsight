package ru.chessinsight.infrastructure.web.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.chessinsight.application.admin.service.AdminUserService;
import ru.chessinsight.application.auth.dto.SignUpDTO;
import ru.chessinsight.domain.common.pagination.PageParams;
import ru.chessinsight.domain.user.model.Role;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.infrastructure.web.api.AdminApi;
import ru.chessinsight.infrastructure.web.dto.AdminPatchUserRequest;
import ru.chessinsight.infrastructure.web.dto.AdminUserDTO;
import ru.chessinsight.infrastructure.web.dto.PageResponse;
import ru.chessinsight.infrastructure.web.dto.UserRole;
import ru.chessinsight.infrastructure.web.mapper.UserApiMapper;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class AdminUserController implements AdminApi, ApiV1Controller {

    private final AdminUserService adminUserService;
    private final UserApiMapper userApiMapper;

    public AdminUserController(AdminUserService adminUserService, UserApiMapper userApiMapper) {
        this.adminUserService = adminUserService;
        this.userApiMapper = userApiMapper;
    }

    @GetMapping("/admin/users")
    public ResponseEntity<PageResponse<AdminUserDTO>> listUsers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) List<UserRole> roles
    ) {
        int p = page != null ? page : 0;
        int s = size != null ? size : 20;

        Set<Role> roleFilter = (roles == null) ? null :
                roles.stream().map(userApiMapper::toDomainRole).collect(Collectors.toSet());

        var resultPage = adminUserService.listUsers(active, roleFilter, new PageParams(p, s));

        List<AdminUserDTO> content = resultPage.content().stream()
                .map(userApiMapper::toAdminUserDto)
                .toList();

        PageResponse<AdminUserDTO> response = new PageResponse<>(
                content,
                resultPage.page(),
                resultPage.size(),
                resultPage.totalElements(),
                resultPage.totalPages(),
                resultPage.hasNext(),
                resultPage.hasPrevious()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/users")
    public ResponseEntity<AdminUserDTO> createUser(
            @Valid @RequestBody ru.chessinsight.infrastructure.web.dto.SignUpDTO body
    ) {
        SignUpDTO signUp = new SignUpDTO(
                body.getLogin(),
                body.getEmail(),
                body.getPassword()
        );

        Set<Role> roles = Set.of(Role.ROLE_USER);

        User user = adminUserService.createUser(signUp, roles, null);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userApiMapper.toAdminUserDto(user));
    }

    @GetMapping("/admin/users/{userId}")
    public ResponseEntity<AdminUserDTO> getUser(@PathVariable UUID userId) {
        User user = adminUserService.getUser(userId);
        return ResponseEntity.ok(userApiMapper.toAdminUserDto(user));
    }

    @PatchMapping("/admin/users/{userId}")
    public ResponseEntity<AdminUserDTO> patchUser(
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

    @DeleteMapping("/admin/users/{userId}")
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID userId) {
        adminUserService.deactivateUser(userId);
        return ResponseEntity.noContent().build();
    }
}
