package ru.chessinsight.domain.user.repository;

import ru.chessinsight.domain.user.model.UserStatistics;

import java.util.Optional;
import java.util.UUID;

public interface UserStatisticsRepository {
    Optional<UserStatistics> save(UserStatistics statistics);
    Optional<UserStatistics> findOneByUserId(UUID userId);
}
