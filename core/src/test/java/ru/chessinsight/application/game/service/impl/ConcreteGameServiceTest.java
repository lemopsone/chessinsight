package ru.chessinsight.application.game.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.chessinsight.application.common.logger.service.Logger;
import ru.chessinsight.application.game.dto.GameMetadataDTO;
import ru.chessinsight.application.game.dto.GameMetadataPatchDTO;
import ru.chessinsight.application.game.dto.GameSearchCriteria;
import ru.chessinsight.application.game.exception.GameNotFoundException;
import ru.chessinsight.domain.common.pagination.Page;
import ru.chessinsight.domain.common.pagination.PageParams;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.model.GameAnalysis;
import ru.chessinsight.domain.game.model.GameResult;
import ru.chessinsight.domain.game.repository.GameRepository;
import ru.chessinsight.testdata.GameAnalysisBuilder;
import ru.chessinsight.testdata.GameBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcreteGameServiceTest {
    @Mock
    private GameRepository gameRepository;
    @Mock
    private Logger logger;

    private ConcreteGameService service;

    @BeforeEach
    void setUp() {
        service = new ConcreteGameService(gameRepository, logger);
    }

    @Test
    void findUserGames_returnsAll() {
        UUID userId = UUID.randomUUID();
        Game g1 = GameBuilder.game().withUserId(userId).build();
        when(gameRepository.findAllByUserId(userId)).thenReturn(List.of(g1));

        List<Game> result = service.findUserGames(userId);

        assertEquals(1, result.size());
        assertEquals(g1, result.get(0));
    }

    @Test
    void findUserGames_returnsEmpty_whenNone() {
        UUID userId = UUID.randomUUID();
        when(gameRepository.findAllByUserId(userId)).thenReturn(List.of());

        List<Game> result = service.findUserGames(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void findUserGames_withPage_returnsPage() {
        UUID userId = UUID.randomUUID();
        PageParams params = new PageParams(0, 2);
        Page<Game> page = new Page<>(List.of(), 0, 2, 0);
        when(gameRepository.findAllByUserId(userId, params)).thenReturn(page);

        Page<Game> result = service.findUserGames(userId, params);

        assertEquals(page, result);
    }

    @Test
    void findUserGames_withPage_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        PageParams params = new PageParams(1, 3);
        Page<Game> page = new Page<>(List.of(), 1, 3, 0);
        when(gameRepository.findAllByUserId(userId, params)).thenReturn(page);

        Page<Game> result = service.findUserGames(userId, params);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
    }

    @Test
    void findUserGames_withCriteria_filters() {
        UUID userId = UUID.randomUUID();
        Game withAnalysis = GameBuilder.game()
                .withUserId(userId)
                .withResult(GameResult.WHITE_WIN)
                .withDate(LocalDate.of(2024, 1, 10))
                .withAnalysis(GameAnalysisBuilder.analysis().build())
                .build();
        Game withoutAnalysis = GameBuilder.game()
                .withUserId(userId)
                .withResult(GameResult.BLACK_WIN)
                .withDate(LocalDate.of(2024, 1, 5))
                .withAnalysis(null)
                .build();
        when(gameRepository.findAllByUserId(userId)).thenReturn(List.of(withAnalysis, withoutAnalysis));

        GameSearchCriteria criteria = new GameSearchCriteria(GameResult.WHITE_WIN, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), true);
        Page<Game> result = service.findUserGames(userId, criteria);

        assertEquals(1, result.content().size());
        assertEquals(withAnalysis, result.content().get(0));
    }

    @Test
    void findUserGames_withCriteria_returnsEmpty_whenNoMatches() {
        UUID userId = UUID.randomUUID();
        Game g = GameBuilder.game().withUserId(userId).withResult(GameResult.BLACK_WIN).build();
        when(gameRepository.findAllByUserId(userId)).thenReturn(List.of(g));

        GameSearchCriteria criteria = new GameSearchCriteria(GameResult.WHITE_WIN, null, null, null);
        Page<Game> result = service.findUserGames(userId, criteria);

        assertTrue(result.content().isEmpty());
    }

    @Test
    void findUserGames_withCriteriaAndPage_pagesResults() {
        UUID userId = UUID.randomUUID();
        Game g1 = GameBuilder.game().withUserId(userId).withDate(LocalDate.of(2024, 1, 1)).build();
        Game g2 = GameBuilder.game().withUserId(userId).withDate(LocalDate.of(2024, 1, 2)).build();
        when(gameRepository.findAllByUserId(userId)).thenReturn(List.of(g1, g2));

        Page<Game> result = service.findUserGames(userId, new GameSearchCriteria(null, null, null, null), new PageParams(0, 1));

        assertEquals(1, result.content().size());
    }

    @Test
    void findUserGames_withCriteriaAndPage_returnsEmpty_whenPageOutOfRange() {
        UUID userId = UUID.randomUUID();
        Game g1 = GameBuilder.game().withUserId(userId).build();
        when(gameRepository.findAllByUserId(userId)).thenReturn(List.of(g1));

        Page<Game> result = service.findUserGames(userId, new GameSearchCriteria(null, null, null, null), new PageParams(2, 1));

        assertTrue(result.content().isEmpty());
    }

    @Test
    void findDemoGames_returnsDemoUserGames() {
        List<Game> demo = List.of(GameBuilder.game().build());
        when(gameRepository.findAllByUserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))).thenReturn(demo);

        List<Game> result = service.findDemoGames();

        assertEquals(demo, result);
    }

    @Test
    void findDemoGames_returnsEmpty() {
        when(gameRepository.findAllByUserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))).thenReturn(List.of());

        List<Game> result = service.findDemoGames();

        assertTrue(result.isEmpty());
    }

    @Test
    void getGame_returnsOptional() {
        UUID gameId = UUID.randomUUID();
        Game game = GameBuilder.game().withId(gameId).build();
        when(gameRepository.findOneById(gameId)).thenReturn(Optional.of(game));

        Optional<Game> result = service.getGame(gameId);

        assertTrue(result.isPresent());
        assertEquals(game, result.get());
    }

    @Test
    void getGame_returnsEmpty_whenMissing() {
        UUID gameId = UUID.randomUUID();
        when(gameRepository.findOneById(gameId)).thenReturn(Optional.empty());

        Optional<Game> result = service.getGame(gameId);

        assertTrue(result.isEmpty());
    }

    @Test
    void updateGameMetadata_updatesAllFields() {
        UUID gameId = UUID.randomUUID();
        Game game = GameBuilder.game().withId(gameId).build();
        when(gameRepository.findOneById(gameId)).thenReturn(Optional.of(game));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameMetadataDTO dto = new GameMetadataDTO("E", "S", LocalDate.of(2024, 2, 2), "3", "W", "B", GameResult.DRAW);
        Game result = service.updateGameMetadata(gameId, dto);

        assertEquals("E", result.getEvent());
        assertEquals("S", result.getSite());
        assertEquals(LocalDate.of(2024, 2, 2), result.getDate());
        assertEquals("3", result.getRound());
        assertEquals("W", result.getWhiteName());
        assertEquals("B", result.getBlackName());
        assertEquals(GameResult.DRAW, result.getResult());
    }

    @Test
    void updateGameMetadata_throws_whenMissing() {
        UUID gameId = UUID.randomUUID();
        when(gameRepository.findOneById(gameId)).thenReturn(Optional.empty());

        assertThrows(GameNotFoundException.class, () -> service.updateGameMetadata(gameId, new GameMetadataDTO(null, null, null, null, null, null, null)));
    }

    @Test
    void patchGameMetadata_updatesOnlyProvided() {
        UUID gameId = UUID.randomUUID();
        Game game = GameBuilder.game().withId(gameId).withEvent("old").build();
        when(gameRepository.findOneById(gameId)).thenReturn(Optional.of(game));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameMetadataPatchDTO dto = new GameMetadataPatchDTO("new", null, null, null, GameResult.WHITE_WIN);
        Game result = service.patchGameMetadata(gameId, dto);

        assertEquals("new", result.getEvent());
        assertEquals(GameResult.WHITE_WIN, result.getResult());
    }

    @Test
    void patchGameMetadata_throws_whenMissing() {
        UUID gameId = UUID.randomUUID();
        when(gameRepository.findOneById(gameId)).thenReturn(Optional.empty());

        assertThrows(GameNotFoundException.class, () -> service.patchGameMetadata(gameId, new GameMetadataPatchDTO(null, null, null, null, null)));
    }

    @Test
    void deleteGame_deletesWhenFound() {
        UUID gameId = UUID.randomUUID();
        Game game = GameBuilder.game().withId(gameId).build();
        when(gameRepository.findOneById(gameId)).thenReturn(Optional.of(game));

        service.deleteGame(gameId);

        verify(gameRepository).delete(game);
    }

    @Test
    void deleteGame_throws_whenMissing() {
        UUID gameId = UUID.randomUUID();
        when(gameRepository.findOneById(gameId)).thenReturn(Optional.empty());

        assertThrows(GameNotFoundException.class, () -> service.deleteGame(gameId));
    }
}
