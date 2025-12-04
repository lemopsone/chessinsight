package ru.chessinsight.infrastructure.persistence.jpa.mapper;

import org.springframework.stereotype.Component;
import ru.chessinsight.domain.user.model.UserStatistics;
import ru.chessinsight.infrastructure.persistence.jpa.model.UserStatisticsEntity;

@Component
public class UserStatisticsMapper implements EntityMapper<UserStatistics, UserStatisticsEntity> {
    @Override
    public UserStatistics toDomain(UserStatisticsEntity e) {
        if (e == null) return null;
        return new UserStatistics(e.getAccuracy(), e.getAccuracyWhite(), e.getAccuracyBlack());
    }

    @Override
    public UserStatisticsEntity toEntity(UserStatistics d) {
        if (d == null) return null;
        var e = new UserStatisticsEntity();
        e.setAccuracy(d.accuracy());
        e.setAccuracyWhite(d.accuracyWhite());
        e.setAccuracyBlack(d.accuracyBlack());
        return e;
    }
}
