package ru.chessinsight.infrastructure.web.mapper;

import org.springframework.stereotype.Component;
import ru.chessinsight.domain.user.model.Role;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.user.model.UserStatistics;
import ru.chessinsight.infrastructure.web.dto.AdminUserDTO;
import ru.chessinsight.infrastructure.web.dto.UserDTO;
import ru.chessinsight.infrastructure.web.dto.UserRole;
import ru.chessinsight.infrastructure.web.dto.UserStatisticsDTO;

import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class UserApiMapper {

    public UserDTO toUserDto(User user) {
        if (user == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setLogin(user.getLogin());
        dto.setEmail(user.getEmail());

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
        UserDTO base = toUserDto(user);
        AdminUserDTO dto = new AdminUserDTO();
        dto.setId(base.getId());
        dto.setLogin(base.getLogin());
        dto.setEmail(base.getEmail());
        dto.setRoles(base.getRoles());
        dto.setStatistics(base.getStatistics());
        dto.setActive(user.isActive());
        return dto;
    }

    public UserStatisticsDTO toStatisticsDto(UserStatistics stats) {
        if (stats == null) {
            return null;
        }
        UserStatisticsDTO dto = new UserStatisticsDTO();
        dto.setAccuracy(stats.accuracy());
        dto.setAccuracyWhite(stats.accuracyWhite());
        dto.setAccuracyBlack(stats.accuracyBlack());
        return dto;
    }

    public UserRole toUserRole(Role role) {
        if (role == null) {
            return null;
        }
        // ROLE_USER -> USER
        String name = role.name();
        if (name.startsWith("ROLE_")) {
            name = name.substring("ROLE_".length());
        }
        return UserRole.valueOf(name);
    }

    public Role toDomainRole(UserRole role) {
        if (role == null) {
            return null;
        }
        return Role.valueOf("ROLE_" + role.name());
    }

    public UUID safeUuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }
}
