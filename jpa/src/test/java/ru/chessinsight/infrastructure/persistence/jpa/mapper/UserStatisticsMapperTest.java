package ru.chessinsight.infrastructure.persistence.jpa.mapper;

import org.junit.jupiter.api.Test;
import ru.chessinsight.domain.user.model.UserStatistics;
import ru.chessinsight.infrastructure.persistence.jpa.model.UserStatisticsEntity;
import io.qameta.allure.Tag;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class UserStatisticsMapperTest {
    private final UserStatisticsMapper mapper = new UserStatisticsMapper();

    @Test
    void toDomain_mapsStatistics() {
        UserStatisticsEntity entity = new UserStatisticsEntity();
        entity.setAccuracy(0.9);
        entity.setAccuracyWhite(0.85);
        entity.setAccuracyBlack(0.95);

        UserStatistics result = mapper.toDomain(entity);

        assertEquals(0.9, result.accuracy());
        assertEquals(0.85, result.accuracyWhite());
        assertEquals(0.95, result.accuracyBlack());
    }

    @Test
    void toDomain_returnsNull_whenEntityNull() {
        UserStatisticsEntity entity = null;

        UserStatistics result = mapper.toDomain(entity);

        assertNull(result);
    }

    @Test
    void toEntity_mapsStatistics() {
        UserStatistics domain = new UserStatistics(0.7, 0.65, 0.75);

        UserStatisticsEntity result = mapper.toEntity(domain);

        assertEquals(0.7, result.getAccuracy());
        assertEquals(0.65, result.getAccuracyWhite());
        assertEquals(0.75, result.getAccuracyBlack());
    }

    @Test
    void toEntity_returnsNull_whenDomainNull() {
        UserStatistics domain = null;

        UserStatisticsEntity result = mapper.toEntity(domain);

        assertNull(result);
    }
}