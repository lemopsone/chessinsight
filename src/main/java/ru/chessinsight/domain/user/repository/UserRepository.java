package ru.chessinsight.domain.user.repository;

import org.jmolecules.ddd.annotation.Repository;
import ru.chessinsight.domain.user.model.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository {
    Optional<User> findOneById(UUID id);
    Optional<User> findOneByLoginOrEmail(String loginOrEmail);
    Optional<User> save(User user);
}
