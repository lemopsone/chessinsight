package ru.chessinsight.infrastructure.persistence.jpa.hibernate;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.chessinsight.infrastructure.persistence.jpa.model.TrainingScenarioEntity;

import java.util.List;
import java.util.UUID;

public interface TrainingScenarioJpaRepository extends JpaRepository<TrainingScenarioEntity, UUID> {
    List<TrainingScenarioEntity> findAllByUserId(UUID userId);
    List<TrainingScenarioEntity> findAllByGameId(UUID gameId);
    List<TrainingScenarioEntity> findAllByUserIdAndCompleted(UUID userId, boolean completed);
}
