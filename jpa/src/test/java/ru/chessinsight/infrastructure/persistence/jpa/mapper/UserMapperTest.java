package ru.chessinsight.infrastructure.persistence.jpa.mapper;

import org.junit.jupiter.api.Test;
import ru.chessinsight.domain.user.model.Role;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.user.model.UserStatistics;
import ru.chessinsight.infrastructure.persistence.jpa.model.UserEntity;
import ru.chessinsight.testdata.UserBuilder;
import ru.chessinsight.testdata.UserEntityMother;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class UserMapperTest {
    private final UserStatisticsMapper statisticsMapper = new UserStatisticsMapper();
    private final UserMapper mapper = new UserMapper(statisticsMapper);

    @Test
    void toDomain_mapsAllFields() {
        UserEntity entity = UserEntityMother.fullUserEntity();

        User result = mapper.toDomain(entity);

        assertEquals(entity.getId(), result.getId());
        assertEquals(entity.getLogin(), result.getLogin());
        assertEquals(entity.getEmail(), result.getEmail());
        assertEquals(entity.getPasswordHash(), result.getPasswordHash());
        assertEquals(entity.isActive(), result.isActive());
        assertNotNull(result.getStatistics());
        assertEquals(0.91, result.getStatistics().accuracy());
        assertEquals(0.88, result.getStatistics().accuracyWhite());
        assertEquals(0.94, result.getStatistics().accuracyBlack());
        assertTrue(result.getRoles().contains(Role.ROLE_USER));
        assertTrue(result.getRoles().contains(Role.ROLE_ADMIN));
    }

    @Test
    void toDomain_returnsNull_whenEntityNull() {
        UserEntity entity = null;

        User result = mapper.toDomain(entity);

        assertNull(result);
    }

    @Test
    void toEntity_mapsAllFields() {
        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        User domain = UserBuilder.user()
                .withId(id)
                .withLogin("bob")
                .withEmail("bob@example.com")
                .withPasswordHash("ph")
                .withStatistics(new UserStatistics(0.6, 0.55, 0.65))
                .withRoles(EnumSet.of(Role.ROLE_USER, Role.ROLE_ADMIN))
                .withActive(false)
                .build();

        UserEntity result = mapper.toEntity(domain);

        assertEquals(domain.getId(), result.getId());
        assertEquals(domain.getLogin(), result.getLogin());
        assertEquals(domain.getEmail(), result.getEmail());
        assertEquals(domain.getPasswordHash(), result.getPasswordHash());
        assertEquals(domain.isActive(), result.isActive());
        assertNotNull(result.getStatistics());
        assertEquals(0.6, result.getStatistics().getAccuracy());
        assertEquals(0.55, result.getStatistics().getAccuracyWhite());
        assertEquals(0.65, result.getStatistics().getAccuracyBlack());
        assertEquals(2, result.getRoles().size());
        Set<String> roles = result.getRoles().stream().map(r -> r.getRole()).collect(java.util.stream.Collectors.toSet());
        assertTrue(roles.contains("ROLE_USER"));
        assertTrue(roles.contains("ROLE_ADMIN"));
    }

    @Test
    void toEntity_returnsNull_whenDomainNull() {
        User domain = null;

        UserEntity result = mapper.toEntity(domain);

        assertNull(result);
    }
}