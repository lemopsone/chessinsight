package ru.chessinsight.application.admin.service.impl;

import org.springframework.stereotype.Service;
import ru.chessinsight.application.admin.service.AdminUserService;
import ru.chessinsight.application.auth.dto.SignUpDTO;
import ru.chessinsight.application.auth.exception.UserExistsException;
import ru.chessinsight.application.auth.service.AuthService;
import ru.chessinsight.application.common.logger.service.Logger;
import ru.chessinsight.application.statistics.exception.UserNotFoundException;
import ru.chessinsight.domain.common.pagination.Page;
import ru.chessinsight.domain.common.pagination.PageParams;
import ru.chessinsight.domain.user.model.Role;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.user.repository.UserRepository;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Service
public class DefaultAdminUserService implements AdminUserService {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final Logger logger;

    public DefaultAdminUserService(UserRepository userRepository,
                                   AuthService authService,
                                   Logger logger) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.logger = logger;
    }

    @Override
    public Page<User> listUsers(Boolean active, Set<Role> roles, PageParams pageRequest) {
        return userRepository.findAll(pageRequest, active, roles);
    }

    @Override
    public User getUser(UUID id) {
        return userRepository.findOneById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Override
    public User createUser(SignUpDTO signUp, Set<Role> roles, Boolean active) {
        try {
            authService.signUp(signUp);
        } catch (UserExistsException e) {
            logger.warning("admin.user.create rejected login=" + signUp.login());
            throw new UserExistsException("User already exists");
        }

        User user = userRepository.findOneByLogin(signUp.login())
                .orElseThrow(() -> new UserNotFoundException("User was created but not found"));

        if (roles != null && !roles.isEmpty()) {
            user.setRoles(roles);
        }
        if (active != null) {
            user.setActive(active);
        }

        User saved = userRepository.save(user);
        logger.info("admin.user.create userId=" + saved.getId());
        return saved;
    }

    @Override
    public User patchUser(UUID id, String login, String email, Set<Role> roles, Boolean active) {
        User user = getUser(id);

        if (login != null) {
            user.setLogin(login);
        }
        if (email != null) {
            user.setEmail(email);
        }
        if (roles != null) {
            user.setRoles(roles);
        }
        if (active != null) {
            user.setActive(active);
        }

        User saved = userRepository.save(user);
        logger.info("admin.user.patch userId=" + id);
        return saved;
    }

    @Override
    public void deactivateUser(UUID id) {
        User user = getUser(id);
        user.setActive(false);
        userRepository.save(user);
        logger.info("admin.user.deactivate userId=" + id);
    }
}
