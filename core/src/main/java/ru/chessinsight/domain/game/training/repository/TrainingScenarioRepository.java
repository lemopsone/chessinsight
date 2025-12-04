package ru.chessinsight.domain.game.training.repository;

import ru.chessinsight.domain.common.pagination.Page;
import ru.chessinsight.domain.common.pagination.PageParams;
import ru.chessinsight.domain.game.training.model.TrainingScenario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingScenarioRepository {
    TrainingScenario save(TrainingScenario scenario);
    Optional<TrainingScenario> findOneById(UUID id);
    List<TrainingScenario> findAllByUserId(UUID userId);
    Page<TrainingScenario> findAllByUserId(UUID userId, PageParams params);
    List<TrainingScenario> findAllByGameId(UUID gameId);
    List<TrainingScenario> findAllByCompletionForUser(UUID userId, Boolean completed);
    Page<TrainingScenario> findAllByCompletionForUser(UUID userId, Boolean completed, PageParams params);
}
