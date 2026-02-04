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
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.repository.GameRepository;
import ru.chessinsight.infrastructure.persistence.jpa.hibernate.GameJpaRepository;
import ru.chessinsight.infrastructure.persistence.jpa.mapper.EntityMapper;
import ru.chessinsight.infrastructure.persistence.jpa.model.GameEntity;
import ru.chessinsight.testdata.GameBuilder;
import ru.chessinsight.testdata.GameEntityMother;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PgGameRepositoryTest {
    @Mock
    private EntityMapper<Game, GameEntity> mapper;
    @Mock
    private GameJpaRepository jpaRepository;
    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    private GameRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PgGameRepository(mapper, jpaRepository);
    }

    @Test
    void save_mapsDomainToEntity_andBack() {
        Game game = GameBuilder.game().build();
        GameEntity entity = GameEntityMother.fullGameEntity();
        when(mapper.toEntity(game)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(game);

        Game result = repository.save(game);

        assertEquals(game, result);
        verify(jpaRepository).save(entity);
    }

    @Test
    void save_throws_whenMapperFails() {
        Game game = GameBuilder.game().build();
        when(mapper.toEntity(game)).thenThrow(new IllegalStateException("map fail"));

        assertThrows(IllegalStateException.class, () -> repository.save(game));
    }

    @Test
    void findOneById_returnsMappedGame_whenFound() {
        GameEntity entity = GameEntityMother.fullGameEntity();
        UUID id = entity.getId();
        Game game = GameBuilder.game().withId(id).build();
        when(jpaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(game);

        Optional<Game> result = repository.findOneById(id);

        assertTrue(result.isPresent());
        assertEquals(game, result.get());
    }

    @Test
    void findOneById_returnsEmpty_whenMissing() {
        UUID id = UUID.randomUUID();
        when(jpaRepository.findById(id)).thenReturn(Optional.empty());

        Optional<Game> result = repository.findOneById(id);

        assertTrue(result.isEmpty());
        verifyNoInteractions(mapper);
    }

    @Test
    void findAllByUserId_mapsAll() {
        GameEntity entity = GameEntityMother.fullGameEntity();
        Game game = GameBuilder.game().withId(entity.getId()).build();
        when(jpaRepository.findAllByUserId(entity.getUserId())).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(game);

        List<Game> result = repository.findAllByUserId(entity.getUserId());

        assertEquals(1, result.size());
        assertEquals(game, result.get(0));
    }

    @Test
    void findAllByUserId_returnsEmpty_whenNone() {
        UUID userId = UUID.randomUUID();
        when(jpaRepository.findAllByUserId(userId)).thenReturn(List.of());

        List<Game> result = repository.findAllByUserId(userId);

        assertTrue(result.isEmpty());
        verifyNoInteractions(mapper);
    }

    @Test
    void findAllByUserId_withPage_mapsAll() {
        UUID userId = UUID.randomUUID();
        PageParams params = new PageParams(0, 2);
        GameEntity entity = GameEntityMother.fullGameEntity();
        Game game = GameBuilder.game().withId(entity.getId()).build();
        when(jpaRepository.findAllByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 2), 1));
        when(mapper.toDomain(entity)).thenReturn(game);

        Page<Game> result = repository.findAllByUserId(userId, params);

        assertEquals(1, result.content().size());
        assertEquals(game, result.content().get(0));
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

        Page<Game> result = repository.findAllByUserId(userId, params);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
        verifyNoInteractions(mapper);
    }

    @Test
    void findAllByPgn_mapsAll() {
        GameEntity entity = GameEntityMother.fullGameEntity();
        Game game = GameBuilder.game().withId(entity.getId()).build();
        when(jpaRepository.findAllByPgn("pgn")).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(game);

        List<Game> result = repository.findAllByPgn("pgn");

        assertEquals(1, result.size());
        assertEquals(game, result.get(0));
    }

    @Test
    void findAllByPgn_returnsEmpty_whenNone() {
        when(jpaRepository.findAllByPgn("missing")).thenReturn(List.of());

        List<Game> result = repository.findAllByPgn("missing");

        assertTrue(result.isEmpty());
        verifyNoInteractions(mapper);
    }

    @Test
    void delete_deletesMappedEntity() {
        Game game = GameBuilder.game().build();
        GameEntity entity = GameEntityMother.fullGameEntity();
        when(mapper.toEntity(game)).thenReturn(entity);

        repository.delete(game);

        verify(jpaRepository).delete(entity);
    }

    @Test
    void delete_throws_whenMapperFails() {
        Game game = GameBuilder.game().build();
        when(mapper.toEntity(game)).thenThrow(new IllegalArgumentException("bad map"));

        assertThrows(IllegalArgumentException.class, () -> repository.delete(game));
    }
}
