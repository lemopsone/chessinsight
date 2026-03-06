package ru.chessinsight.infrastructure.persistence.jpa.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import ru.chessinsight.domain.common.pagination.Page;
import ru.chessinsight.domain.common.pagination.PageParams;
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

    public PgTrainingScenarioRepository(
            TrainingScenarioJpaRepository jpaRepository,
            EntityMapper<TrainingScenario, TrainingScenarioEntity> mapper
    ) {
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
    public Page<TrainingScenario> findAllByUserId(UUID userId, PageParams params) {
        Pageable pageable = PageRequest.of(params.page(), params.size());

        org.springframework.data.domain.Page<TrainingScenarioEntity> springPage =
                jpaRepository.findAllByUserId(userId, pageable);

        List<TrainingScenario> content = springPage.getContent().stream()
                .map(mapper::toDomain)
                .toList();

        return new Page<>(
                content,
                params.page(),
                params.size(),
                springPage.getTotalElements()
        );
    }

    @Override
    public List<TrainingScenario> findAllByGameId(UUID gameId) {
        return jpaRepository.findAllByGameId(gameId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<TrainingScenario> findAllByCompletionForUser(UUID userId, Boolean completed) {
        if (completed != null) {
            return jpaRepository.findAllByUserIdAndCompleted(userId, completed).stream().map(mapper::toDomain).toList();
        }
        return jpaRepository.findAllByUserId(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Page<TrainingScenario> findAllByCompletionForUser(UUID userId, Boolean completed, PageParams params) {
        Pageable pageable = PageRequest.of(params.page(), params.size());

        org.springframework.data.domain.Page<TrainingScenarioEntity> springPage;
        if (completed != null) {
            springPage = jpaRepository.findAllByUserIdAndCompleted(userId, completed, pageable);
        } else {
            springPage = jpaRepository.findAllByUserId(userId, pageable);
        }

        List<TrainingScenario> content = springPage.getContent().stream()
                .map(mapper::toDomain)
                .toList();

        return new Page<>(
                content,
                params.page(),
                params.size(),
                springPage.getTotalElements()
        );
    }
}
