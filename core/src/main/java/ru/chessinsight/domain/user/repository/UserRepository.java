package ru.chessinsight.domain.user.repository;

import ru.chessinsight.domain.common.pagination.Page;
import ru.chessinsight.domain.common.pagination.PageParams;
import ru.chessinsight.domain.user.model.Role;
import ru.chessinsight.domain.user.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findOneById(UUID id);
    Optional<User> findOneByLogin(String login);
    Optional<User> findOneByEmail(String email);
    List<User> findAll();
    Page<User> findAll(PageParams params, Boolean active, Set<Role> roles);
    User save(User user);
}
