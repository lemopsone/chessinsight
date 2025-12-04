package ru.chessinsight.infrastructure.persistence.jpa.repository;

import org.springframework.stereotype.Repository;
import ru.chessinsight.domain.game.training.model.TrainingScenario;
import ru.chessinsight.domain.game.training.repository.TrainingScenarioRepository;
import ru.chessinsight.infrastructure.persistence.jpa.hibernate.TrainingScenarioJpaRepository;
import ru.chessinsight.infrastructure.persistence.jpa.mapper.EntityMapper;
import ru.chessinsight.infrastructure.persistence.jpa.model.TrainingScenarioEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PgTrainingScenarioRepository implements TrainingScenarioRepository {
    private final TrainingScenarioJpaRepository jpaRepository;
    private final EntityMapper<TrainingScenario, TrainingScenarioEntity> mapper;

    public PgTrainingScenarioRepository(TrainingScenarioJpaRepository jpaRepository, EntityMapper<TrainingScenario, TrainingScenarioEntity> mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public TrainingScenario save(TrainingScenario scenario) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(scenario)));
    }

    @Override
    public Optional<TrainingScenario> findOneById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<TrainingScenario> findAllByUserId(UUID userId) {
        return jpaRepository.findAllByUserId(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<TrainingScenario> findAllByGameId(UUID gameId) {
        return jpaRepository.findAllByGameId(gameId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<TrainingScenario> findAllUncompletedForUser(UUID userId) {
        return jpaRepository.findAllByUserIdAndCompleted(userId, false).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<TrainingScenario> findAllCompletedForUser(UUID userId) {
        return jpaRepository.findAllByUserIdAndCompleted(userId, true).stream().map(mapper::toDomain).toList();
    }
}
