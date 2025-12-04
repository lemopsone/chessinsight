package ru.chessinsight.infrastructure.persistence.jpa.mapper;

import org.springframework.stereotype.Component;
import ru.chessinsight.domain.user.model.Role;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.infrastructure.persistence.jpa.model.UserEntity;
import ru.chessinsight.infrastructure.persistence.jpa.model.UserRoleEntity;

import java.util.stream.Collectors;

@Component
public class UserMapper implements EntityMapper<User, UserEntity> {
    private final UserStatisticsMapper statisticsMapper;

    public UserMapper(UserStatisticsMapper statisticsMapper) {
        this.statisticsMapper = statisticsMapper;
    }

    @Override
    public User toDomain(UserEntity e) {
        if (e == null) return null;
        var stats = statisticsMapper.toDomain(e.getStatistics());
        var user = new User();
        user.setId(e.getId());
        user.setLogin(e.getLogin());
        user.setEmail(e.getEmail());
        user.setPasswordHash(e.getPasswordHash());
        user.setStatistics(stats);
        user.setRoles(e.getRoles().stream().map(r -> Role.valueOf(r.getRole())).collect(Collectors.toSet()));
        return user;
    }

    @Override
    public UserEntity toEntity(User d) {
        if (d == null) return null;
        var e = new UserEntity();
        var stats = statisticsMapper.toEntity(d.getStatistics());
        e.setId(d.getId());
        e.setLogin(d.getLogin());
        e.setEmail(d.getEmail());
        e.setPasswordHash(d.getPasswordHash());
        e.setStatistics(stats);
        if (d.getRoles() != null) {
            d.getRoles().forEach(r -> {
                var re = new UserRoleEntity();
                re.setRole(r.name());
                e.addRole(re);
            });
        }
        return e;
    }
}
