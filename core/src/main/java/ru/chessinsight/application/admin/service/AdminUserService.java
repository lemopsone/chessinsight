package ru.chessinsight.application.admin.service;

import ru.chessinsight.domain.common.pagination.Page;
import ru.chessinsight.domain.common.pagination.PageParams;
import ru.chessinsight.application.auth.dto.SignUpDTO;
import ru.chessinsight.domain.user.model.Role;
import ru.chessinsight.domain.user.model.User;

import java.util.Set;
import java.util.UUID;

public interface AdminUserService {

    Page<User> listUsers(Boolean active, Set<Role> roles, PageParams params);

    User getUser(UUID id);

    User createUser(SignUpDTO signUp, Set<Role> roles, Boolean active);

    User patchUser(UUID id, String login, String email, Set<Role> roles, Boolean active);

    void deactivateUser(UUID id);
}
