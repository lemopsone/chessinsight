package ru.chessinsight.infrastructure.persistence.jpa.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_role")
public class UserRoleEntity {
    @Id
    @Column
    private String role;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserEntity user;

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
}
