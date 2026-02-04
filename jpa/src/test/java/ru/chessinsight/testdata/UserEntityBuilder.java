package ru.chessinsight.testdata;

import ru.chessinsight.infrastructure.persistence.jpa.model.UserEntity;
import ru.chessinsight.infrastructure.persistence.jpa.model.UserRoleEntity;
import ru.chessinsight.infrastructure.persistence.jpa.model.UserStatisticsEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class UserEntityBuilder {
    private UUID id = UUID.randomUUID();
    private String login = "user";
    private String email = "user@example.com";
    private String passwordHash = "hash";
    private boolean active = true;
    private Set<String> roles = new HashSet<>(Set.of("ROLE_USER"));
    private UserStatisticsEntity statistics = defaultStatistics();

    public static UserEntityBuilder userEntity() {
        return new UserEntityBuilder();
    }

    public UserEntityBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public UserEntityBuilder withLogin(String login) {
        this.login = login;
        return this;
    }

    public UserEntityBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserEntityBuilder withPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        return this;
    }

    public UserEntityBuilder withActive(boolean active) {
        this.active = active;
        return this;
    }

    public UserEntityBuilder withRoles(Set<String> roles) {
        this.roles = roles;
        return this;
    }

    public UserEntityBuilder withStatistics(UserStatisticsEntity statistics) {
        this.statistics = statistics;
        return this;
    }

    public UserEntity build() {
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setLogin(login);
        entity.setEmail(email);
        entity.setPasswordHash(passwordHash);
        entity.setActive(active);
        if (statistics != null) {
            entity.setStatistics(statistics);
        }
        if (roles != null) {
            for (String role : roles) {
                UserRoleEntity roleEntity = new UserRoleEntity();
                roleEntity.setRole(role);
                entity.addRole(roleEntity);
            }
        }
        return entity;
    }

    private static UserStatisticsEntity defaultStatistics() {
        UserStatisticsEntity stats = new UserStatisticsEntity();
        stats.setAccuracy(0.8);
        stats.setAccuracyWhite(0.75);
        stats.setAccuracyBlack(0.85);
        return stats;
    }
}
