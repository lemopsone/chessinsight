package ru.chessinsight.domain.game.training.repository;

import ru.chessinsight.domain.game.training.model.TrainingScenario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingScenarioRepository {
    TrainingScenario save(TrainingScenario scenario);
    Optional<TrainingScenario> findOneById(UUID id);
    List<TrainingScenario> findAllByUserId(UUID userId);
    List<TrainingScenario> findAllByGameId(UUID gameId);
    List<TrainingScenario> findAllUncompletedForUser(UUID userId);
    List<TrainingScenario> findAllCompletedForUser(UUID userId);
}
