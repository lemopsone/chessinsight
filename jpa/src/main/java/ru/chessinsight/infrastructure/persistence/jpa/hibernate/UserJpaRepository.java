package ru.chessinsight.infrastructure.persistence.jpa.hibernate;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.chessinsight.infrastructure.persistence.jpa.model.UserEntity;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByLogin(String login);
    Optional<UserEntity> findByEmail(String email);
}
