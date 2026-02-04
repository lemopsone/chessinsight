package ru.chessinsight.testdata;

import ru.chessinsight.infrastructure.persistence.jpa.model.UserEntity;
import ru.chessinsight.infrastructure.persistence.jpa.model.UserStatisticsEntity;

import java.util.Set;
import java.util.UUID;

public final class UserEntityMother {
    private UserEntityMother() {}

    public static UserEntity fullUserEntity() {
        UserStatisticsEntity stats = new UserStatisticsEntity();
        stats.setAccuracy(0.91);
        stats.setAccuracyWhite(0.88);
        stats.setAccuracyBlack(0.94);

        return UserEntityBuilder.userEntity()
                .withId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .withLogin("alice")
                .withEmail("alice@example.com")
                .withPasswordHash("hash")
                .withActive(true)
                .withRoles(Set.of("ROLE_USER", "ROLE_ADMIN"))
                .withStatistics(stats)
                .build();
    }
}
