package ru.chessinsight.infrastructure.persistence.jpa.hibernate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.chessinsight.infrastructure.persistence.jpa.model.TrainingScenarioEntity;

import java.util.List;
import java.util.UUID;

public interface TrainingScenarioJpaRepository extends JpaRepository<TrainingScenarioEntity, UUID> {
    List<TrainingScenarioEntity> findAllByUserId(UUID userId);
    Page<TrainingScenarioEntity> findAllByUserId(UUID userId, Pageable pageable);
    List<TrainingScenarioEntity> findAllByGameId(UUID gameId);
    Page<TrainingScenarioEntity> findAllByGameId(UUID gameId, Pageable pageable);
    List<TrainingScenarioEntity> findAllByUserIdAndCompleted(UUID userId, boolean completed);
    Page<TrainingScenarioEntity> findAllByUserIdAndCompleted(UUID userId, boolean completed, Pageable pageable);
}
