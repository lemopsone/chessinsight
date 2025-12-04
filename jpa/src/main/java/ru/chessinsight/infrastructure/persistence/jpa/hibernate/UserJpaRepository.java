package ru.chessinsight.infrastructure.persistence.jpa.hibernate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.chessinsight.domain.user.model.Role;
import ru.chessinsight.infrastructure.persistence.jpa.model.UserEntity;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByLogin(String login);
    Optional<UserEntity> findByEmail(String email);

    Page<UserEntity> findAllByActive(boolean active, Pageable pageable);
    Page<UserEntity> findDistinctByRolesIn(Set<Role> roles, Pageable pageable);
    Page<UserEntity> findDistinctByActiveAndRolesIn(boolean active, Set<Role> roles, Pageable pageable);
}
