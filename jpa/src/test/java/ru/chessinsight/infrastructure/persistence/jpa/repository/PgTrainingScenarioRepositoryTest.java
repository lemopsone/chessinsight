package ru.chessinsight.infrastructure.persistence.jpa.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.chessinsight.domain.common.pagination.Page;
import ru.chessinsight.domain.common.pagination.PageParams;
import ru.chessinsight.domain.game.training.model.TrainingScenario;
import ru.chessinsight.domain.game.training.repository.TrainingScenarioRepository;
import ru.chessinsight.infrastructure.persistence.jpa.hibernate.TrainingScenarioJpaRepository;
import ru.chessinsight.infrastructure.persistence.jpa.mapper.EntityMapper;
import ru.chessinsight.infrastructure.persistence.jpa.model.TrainingScenarioEntity;
import ru.chessinsight.testdata.TrainingScenarioBuilder;
import ru.chessinsight.testdata.TrainingScenarioEntityMother;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ru.chessinsight.testutil.Tag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class PgTrainingScenarioRepositoryTest {
    @Mock
    private TrainingScenarioJpaRepository jpaRepository;
    @Mock
    private EntityMapper<TrainingScenario, TrainingScenarioEntity> mapper;
    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    private TrainingScenarioRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PgTrainingScenarioRepository(jpaRepository, mapper);
    }

    @Test
    void save_mapsDomainToEntity_andBack() {
        TrainingScenario scenario = TrainingScenarioBuilder.scenario().build();
        TrainingScenarioEntity entity = TrainingScenarioEntityMother.completedScenarioEntity();
        when(mapper.toEntity(scenario)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(scenario);

        TrainingScenario result = repository.save(scenario);

        assertEquals(scenario, result);
        verify(jpaRepository).save(entity);
    }

    @Test
    void save_throws_whenMapperFails() {
        TrainingScenario scenario = TrainingScenarioBuilder.scenario().build();
        when(mapper.toEntity(scenario)).thenThrow(new IllegalStateException("map fail"));

        assertThrows(IllegalStateException.class, () -> repository.save(scenario));
    }

    @Test
    void findOneById_returnsMappedScenario_whenFound() {
        TrainingScenarioEntity entity = TrainingScenarioEntityMother.completedScenarioEntity();
        TrainingScenario scenario = TrainingScenarioBuilder.scenario().withId(entity.getId()).build();
        when(jpaRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(scenario);

        Optional<TrainingScenario> result = repository.findOneById(entity.getId());

        assertTrue(result.isPresent());
        assertEquals(scenario, result.get());
    }

    @Test
    void findOneById_returnsEmpty_whenMissing() {
        UUID id = UUID.randomUUID();
        when(jpaRepository.findById(id)).thenReturn(Optional.empty());

        Optional<TrainingScenario> result = repository.findOneById(id);

        assertTrue(result.isEmpty());
        verifyNoInteractions(mapper);
    }

    @Test
    void findAllByUserId_mapsAll() {
        TrainingScenarioEntity entity = TrainingScenarioEntityMother.completedScenarioEntity();
        TrainingScenario scenario = TrainingScenarioBuilder.scenario().withId(entity.getId()).build();
        when(jpaRepository.findAllByUserId(entity.getUserId())).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(scenario);

        List<TrainingScenario> result = repository.findAllByUserId(entity.getUserId());

        assertEquals(1, result.size());
        assertEquals(scenario, result.get(0));
    }

    @Test
    void findAllByUserId_returnsEmpty_whenNone() {
        UUID userId = UUID.randomUUID();
        when(jpaRepository.findAllByUserId(userId)).thenReturn(List.of());

        List<TrainingScenario> result = repository.findAllByUserId(userId);

        assertTrue(result.isEmpty());
        verifyNoInteractions(mapper);
    }

    @Test
    void findAllByUserId_withPage_mapsAll() {
        UUID userId = UUID.randomUUID();
        PageParams params = new PageParams(0, 2);
        TrainingScenarioEntity entity = TrainingScenarioEntityMother.completedScenarioEntity();
        TrainingScenario scenario = TrainingScenarioBuilder.scenario().withId(entity.getId()).build();
        when(jpaRepository.findAllByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 2), 1));
        when(mapper.toDomain(entity)).thenReturn(scenario);

        Page<TrainingScenario> result = repository.findAllByUserId(userId, params);

        assertEquals(1, result.content().size());
        assertEquals(scenario, result.content().get(0));
        verify(jpaRepository).findAllByUserId(eq(userId), pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(2, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void findAllByUserId_withPage_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        PageParams params = new PageParams(1, 3);
        when(jpaRepository.findAllByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 3), 0));

        Page<TrainingScenario> result = repository.findAllByUserId(userId, params);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
        verifyNoInteractions(mapper);
    }

    @Test
    void findAllByGameId_mapsAll() {
        TrainingScenarioEntity entity = TrainingScenarioEntityMother.completedScenarioEntity();
        TrainingScenario scenario = TrainingScenarioBuilder.scenario().withId(entity.getId()).build();
        when(jpaRepository.findAllByGameId(entity.getGameId())).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(scenario);

        List<TrainingScenario> result = repository.findAllByGameId(entity.getGameId());

        assertEquals(1, result.size());
        assertEquals(scenario, result.get(0));
    }

    @Test
    void findAllByGameId_returnsEmpty_whenNone() {
        UUID gameId = UUID.randomUUID();
        when(jpaRepository.findAllByGameId(gameId)).thenReturn(List.of());

        List<TrainingScenario> result = repository.findAllByGameId(gameId);

        assertTrue(result.isEmpty());
        verifyNoInteractions(mapper);
    }

    @Test
    void findAllByCompletionForUser_filtersByCompleted() {
        TrainingScenarioEntity entity = TrainingScenarioEntityMother.completedScenarioEntity();
        TrainingScenario scenario = TrainingScenarioBuilder.scenario().withId(entity.getId()).build();
        when(jpaRepository.findAllByUserIdAndCompleted(entity.getUserId(), true)).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(scenario);

        List<TrainingScenario> result = repository.findAllByCompletionForUser(entity.getUserId(), true);

        assertEquals(1, result.size());
        assertEquals(scenario, result.get(0));
    }

    @Test
    void findAllByCompletionForUser_returnsAll_whenCompletedNull() {
        TrainingScenarioEntity entity = TrainingScenarioEntityMother.completedScenarioEntity();
        TrainingScenario scenario = TrainingScenarioBuilder.scenario().withId(entity.getId()).build();
        when(jpaRepository.findAllByUserId(entity.getUserId())).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(scenario);

        List<TrainingScenario> result = repository.findAllByCompletionForUser(entity.getUserId(), null);

        assertEquals(1, result.size());
        assertEquals(scenario, result.get(0));
    }

    @Test
    void findAllByCompletionForUser_withPage_filtersByCompleted() {
        UUID userId = UUID.randomUUID();
        PageParams params = new PageParams(0, 1);
        TrainingScenarioEntity entity = TrainingScenarioEntityMother.completedScenarioEntity();
        TrainingScenario scenario = TrainingScenarioBuilder.scenario().withId(entity.getId()).build();
        when(jpaRepository.findAllByUserIdAndCompleted(eq(userId), eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 1), 1));
        when(mapper.toDomain(entity)).thenReturn(scenario);

        Page<TrainingScenario> result = repository.findAllByCompletionForUser(userId, false, params);

        assertEquals(1, result.content().size());
        assertEquals(scenario, result.content().get(0));
    }

    @Test
    void findAllByCompletionForUser_withPage_returnsAll_whenCompletedNull() {
        UUID userId = UUID.randomUUID();
        PageParams params = new PageParams(1, 2);
        when(jpaRepository.findAllByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 2), 0));

        Page<TrainingScenario> result = repository.findAllByCompletionForUser(userId, null, params);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
    }
}