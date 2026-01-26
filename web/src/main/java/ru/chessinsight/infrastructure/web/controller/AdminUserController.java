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
import ru.chessinsight.infrastructure.web.dto.AdminCreateUserRequest;
import ru.chessinsight.infrastructure.web.dto.AdminPatchUserRequest;
import ru.chessinsight.infrastructure.web.dto.AdminUserDTO;
import ru.chessinsight.infrastructure.web.dto.PageResponseAdminUserDTO;
import ru.chessinsight.infrastructure.web.dto.UserRole;
import ru.chessinsight.infrastructure.web.mapper.UserApiMapper;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
public class AdminUserController implements AdminApi, ApiV1Controller {

    private final AdminUserService adminUserService;
    private final UserApiMapper userApiMapper;

    public AdminUserController(AdminUserService adminUserService, UserApiMapper userApiMapper) {
        this.adminUserService = adminUserService;
        this.userApiMapper = userApiMapper;
    }

    @Override
    @GetMapping("/admin/users")
    public ResponseEntity<PageResponseAdminUserDTO> adminUsersGet(
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
    @PostMapping("/admin/users")
    public ResponseEntity<AdminUserDTO> adminCreateUser(
            @Valid @RequestBody AdminCreateUserRequest body
    ) {
        SignUpDTO signUp = new SignUpDTO(
                body.getLogin(),
                body.getEmail(),
                body.getPassword()
        );

        Set<Role> roles = (body.getRoles() == null || body.getRoles().isEmpty())
                ? Set.of(Role.ROLE_USER)
                : userApiMapper.toDomainRoles(body.getRoles());

        User user = adminUserService.createUser(signUp, roles, null);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userApiMapper.toAdminUserDto(user));
    }

    @Override
    @GetMapping("/admin/users/{userId}")
    public ResponseEntity<AdminUserDTO> adminGetUser(@PathVariable UUID userId) {
        User user = adminUserService.getUser(userId);
        return ResponseEntity.ok(userApiMapper.toAdminUserDto(user));
    }

    @Override
    @PatchMapping("/admin/users/{userId}")
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
    @DeleteMapping("/admin/users/{userId}")
    public ResponseEntity<Void> adminDeactivateUser(@PathVariable UUID userId) {
        adminUserService.deactivateUser(userId);
        return ResponseEntity.noContent().build();
    }
}
