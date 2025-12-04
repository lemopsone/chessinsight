package ru.chessinsight.infrastructure.cli.api;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class SessionContext {

    private String accessToken;
    private String refreshToken;
    private UUID userId;
    private String login;
    private Set<String> roles;

    public boolean isAuthenticated() {
        return accessToken != null;
    }

    public void clear() {
        accessToken = null;
        refreshToken = null;
        userId = null;
        login = null;
        roles = null;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
