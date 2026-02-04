package ru.chessinsight.application.user.service.impl;

import org.junit.jupiter.api.Test;
import ru.chessinsight.application.statistics.exception.UserNotFoundException;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.fixtures.UserProfileServiceFixture;
import ru.chessinsight.testdata.UserBuilder;
import ru.chessinsight.testdata.UserMother;

import java.util.UUID;
import ru.chessinsight.testutil.Tag;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class DefaultUserProfileServiceClassicTest {

    @Test
    void updateProfile_updatesEmailAndPassword_inMemory() {
        UserProfileServiceFixture fixture = new UserProfileServiceFixture();
        UUID userId = UUID.randomUUID();
        User existing = UserBuilder.user()
                .withId(userId)
                .withEmail("old@example.com")
                .withPasswordHash("old")
                .build();
        fixture.seed(existing);

        User result = fixture.service.updateProfile(userId, "new@example.com", "newpass");

        assertEquals("new@example.com", result.getEmail());
        assertEquals("hashed:newpass", result.getPasswordHash());
        User stored = fixture.repository.findOneById(userId).orElseThrow();
        assertEquals("new@example.com", stored.getEmail());
    }

    @Test
    void updateProfile_throws_whenMissing_inMemory() {
        UserProfileServiceFixture fixture = new UserProfileServiceFixture();
        UUID missingId = UserMother.activeUser().getId();

        assertThrows(UserNotFoundException.class,
                () -> fixture.service.updateProfile(missingId, "x@example.com", "pass"));
    }
}