package ru.chessinsight.domain.user.repository;

import ru.chessinsight.domain.user.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findOneById(UUID id);
    Optional<User> findOneByLogin(String login);
    Optional<User> findOneByEmail(String email);
    User save(User user);
}
