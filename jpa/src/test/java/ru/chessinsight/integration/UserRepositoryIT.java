package ru.chessinsight.integration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.chessinsight.domain.user.model.Role;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.user.model.UserStatistics;
import ru.chessinsight.domain.user.repository.UserRepository;

import java.util.EnumSet;
import org.junit.jupiter.api.Tag;

@Tag("integration")
public class UserRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository repo;

    @Test
    void save_and_find_user_by_login_or_email() {
        User u = new User();
        u.setLogin("alice");
        u.setEmail("alice@example.com");
        u.setPasswordHash("ph");
        u.setRoles(EnumSet.of(Role.ROLE_USER));
        UserStatistics s = new UserStatistics(0.8, 0.82, 0.78);
        u.setStatistics(s);

        var saved = repo.save(u);
        Assertions.assertNotNull(saved.getId());

        var byLogin = repo.findOneByLogin("alice").orElseThrow();
        Assertions.assertEquals(saved.getId(), byLogin.getId());

        var byEmail = repo.findOneByEmail("alice@example.com").orElseThrow();
        Assertions.assertEquals(saved.getId(), byEmail.getId());
    }
}