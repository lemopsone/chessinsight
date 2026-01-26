package ru.chessinsight.infrastructure.security.model;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ru.chessinsight.domain.user.model.Role;
import ru.chessinsight.domain.user.model.User;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class UserPassport implements UserDetails {
    private final String id;
    private final String username;
    private final String email;
    private final String password;
    private final Set<Role> roles;

    public UserPassport(User user) {
        this.id = user.getId().toString();
        this.username = user.getLogin();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.roles = user.getRoles();
    }

    public String getId() {
        return id;
    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map((role) -> new SimpleGrantedAuthority(role.toString()))
                .collect(Collectors.toSet());
    }
    @Override
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
