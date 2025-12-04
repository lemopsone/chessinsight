package ru.chessinsight.domain.user.model;

import java.io.Serializable;
import java.util.*;

public class User implements Serializable {
    private UUID id;
    private String login;
    private String email;
    private String passwordHash;
    private UserStatistics statistics;
    private Set<Role> roles = new HashSet<>();

    public User() { }
    public User(UUID id, String login, String email, String passwordHash, UserStatistics statistics, Set<Role> roles) {
        this.id = id;
        this.login = login;
        this.email = email;
        this.passwordHash = passwordHash;
        this.statistics = statistics;
        if (roles != null) {
            this.roles = roles;
        } else {
            roles = new HashSet<>();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(UserStatistics statistics) {
        this.statistics = statistics;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles.clear();
        if (roles != null) {
            this.roles = roles;
        }
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }
}
