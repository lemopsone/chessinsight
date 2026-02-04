package ru.chessinsight.testdata;

import ru.chessinsight.domain.user.model.User;

import java.util.UUID;

public final class UserMother {
    private UserMother() {}

    public static User activeUser() {
        return UserBuilder.user().build();
    }

    public static User inactiveUser() {
        return UserBuilder.user().withActive(false).build();
    }

    public static User userWithId(UUID id) {
        return UserBuilder.user().withId(id).build();
    }
}
