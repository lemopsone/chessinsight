package ru.chessinsight.application.statistics.service;

import org.springframework.stereotype.Service;
import ru.chessinsight.application.statistics.exception.UserNotFoundException;
import ru.chessinsight.domain.user.model.UserStatistics;

import java.util.UUID;

@Service
public interface StatisticsService {
    UserStatistics getUserStatistics(UUID userId) throws UserNotFoundException;
}
