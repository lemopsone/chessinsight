package ru.chessinsight.testdata;

import ru.chessinsight.domain.user.model.Role;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.user.model.UserStatistics;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public class UserBuilder {
    private UUID id = UUID.randomUUID();
    private String login = "user";
    private String email = "user@example.com";
    private String passwordHash = "hash";
    private UserStatistics statistics = new UserStatistics(0.8, 0.75, 0.85);
    private Set<Role> roles = EnumSet.of(Role.ROLE_USER);
    private boolean active = true;

    public static UserBuilder user() {
        return new UserBuilder();
    }

    public UserBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public UserBuilder withLogin(String login) {
        this.login = login;
        return this;
    }

    public UserBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserBuilder withPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        return this;
    }

    public UserBuilder withStatistics(UserStatistics statistics) {
        this.statistics = statistics;
        return this;
    }

    public UserBuilder withRoles(Set<Role> roles) {
        this.roles = roles;
        return this;
    }

    public UserBuilder withActive(boolean active) {
        this.active = active;
        return this;
    }

    public User build() {
        return new User(id, login, email, passwordHash, statistics, roles, active);
    }
}
