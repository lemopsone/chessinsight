package ru.chessinsight.infrastructure.cli.api.dto;

import java.util.List;
import java.util.UUID;

public class ApiUser {
    private UUID id;
    private String login;
    private String email;
    private boolean active;
    private List<String> roles;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
}
