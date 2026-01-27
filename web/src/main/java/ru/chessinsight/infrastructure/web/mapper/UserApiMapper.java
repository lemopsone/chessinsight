package ru.chessinsight.infrastructure.web.mapper;

import org.springframework.stereotype.Component;
import ru.chessinsight.domain.common.pagination.Page;
import ru.chessinsight.domain.user.model.Role;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.user.model.UserStatistics;
import ru.chessinsight.infrastructure.web.dto.AdminUserDTO;
import ru.chessinsight.infrastructure.web.dto.PageResponseAdminUserDTO;
import ru.chessinsight.infrastructure.web.dto.UserDTO;
import ru.chessinsight.infrastructure.web.dto.UserRole;
import ru.chessinsight.infrastructure.web.dto.UserStatisticsDTO;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserApiMapper {

    public UserDTO toUserDto(User user) {
        if (user == null) return null;

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setLogin(user.getLogin());
        dto.setEmail(user.getEmail());
        dto.setActive(user.isActive());

        if (user.getRoles() != null) {
            dto.setRoles(
                    user.getRoles().stream()
                            .map(this::toUserRole)
                            .collect(Collectors.toList())
            );
        }

        dto.setStatistics(toStatisticsDto(user.getStatistics()));

        return dto;
    }

    public AdminUserDTO toAdminUserDto(User user) {
        if (user == null) return null;

        AdminUserDTO dto = new AdminUserDTO();
        dto.setId(user.getId());
        dto.setLogin(user.getLogin());
        dto.setEmail(user.getEmail());
        dto.setActive(user.isActive());

        if (user.getRoles() != null) {
            dto.setRoles(
                    user.getRoles().stream()
                            .map(this::toUserRole)
                            .collect(Collectors.toList())
            );
        }

        dto.setStatistics(toStatisticsDto(user.getStatistics()));
        return dto;
    }

    public PageResponseAdminUserDTO toAdminUserPageResponse(Page<User> page) {
        PageResponseAdminUserDTO response = new PageResponseAdminUserDTO();
        response.setContent(page.content().stream().map(this::toAdminUserDto).toList());
        response.setPage(page.page());
        response.setSize(page.size());
        response.setTotalElements(page.totalElements());
        response.setTotalPages(page.totalPages());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());
        return response;
    }

    public UserStatisticsDTO toStatisticsDto(UserStatistics stats) {
        if (stats == null) return null;
        UserStatisticsDTO dto = new UserStatisticsDTO();
        dto.setAccuracy(stats.accuracy());
        dto.setAccuracyWhite(stats.accuracyWhite());
        dto.setAccuracyBlack(stats.accuracyBlack());
        return dto;
    }

    public UserRole toUserRole(Role role) {
        if (role == null) return null;
        String name = role.name();
        if (name.startsWith("ROLE_")) {
            name = name.substring("ROLE_".length());
        }
        return UserRole.valueOf(name);
    }

    public Role toDomainRole(UserRole role) {
        if (role == null) return null;
        return Role.valueOf("ROLE_" + role.name());
    }

    public Set<Role> toDomainRoles(List<UserRole> roles) {
        if (roles == null) return null;
        return roles.stream()
                .map(this::toDomainRole)
                .collect(Collectors.toSet());
    }
}
