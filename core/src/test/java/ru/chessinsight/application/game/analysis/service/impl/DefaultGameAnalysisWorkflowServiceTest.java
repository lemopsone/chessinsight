package ru.chessinsight.application.game.analysis.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.chessinsight.application.game.analysis.service.AnalysisService;
import ru.chessinsight.application.game.analysis.service.GameAnalysisWorkflowService;
import ru.chessinsight.application.game.dto.GameAnalysisDTO;
import ru.chessinsight.application.game.exception.GameNotFoundException;
import ru.chessinsight.application.game.service.GameService;
import ru.chessinsight.application.game.training.service.TrainingService;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.testdata.GameAnalysisBuilder;
import ru.chessinsight.testdata.GameBuilder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultGameAnalysisWorkflowServiceTest {
    @Mock
    private GameService gameService;
    @Mock
    private AnalysisService analysisService;
    @Mock
    private TrainingService trainingService;

    private GameAnalysisWorkflowService service;

    @BeforeEach
    void setUp() {
        service = new DefaultGameAnalysisWorkflowService(gameService, analysisService, trainingService);
    }

    @Test
    void analyzeGame_runsTraining_whenNoAnalysis() {
        UUID gameId = UUID.randomUUID();
        Game game = GameBuilder.game().withId(gameId).withAnalysis(null).build();
        GameAnalysisDTO dto = new GameAnalysisDTO(gameId, game.getUserId(), 90.0, 88.0, List.of(), List.of(), List.of(), List.of(), List.of());
        when(gameService.getGame(gameId)).thenReturn(Optional.of(game));
        when(analysisService.analyzeGame(game)).thenReturn(dto);

        GameAnalysisDTO result = service.analyzeGame(gameId);

        assertEquals(dto, result);
        verify(trainingService).createScenariosFromAnalysis(dto);
    }

    @Test
    void analyzeGame_skipsTraining_whenAnalysisExists() {
        UUID gameId = UUID.randomUUID();
        Game game = GameBuilder.game().withId(gameId).withAnalysis(GameAnalysisBuilder.analysis().build()).build();
        GameAnalysisDTO dto = new GameAnalysisDTO(gameId, game.getUserId(), 90.0, 88.0, List.of(), List.of(), List.of(), List.of(), List.of());
        when(gameService.getGame(gameId)).thenReturn(Optional.of(game));
        when(analysisService.analyzeGame(game)).thenReturn(dto);

        GameAnalysisDTO result = service.analyzeGame(gameId);

        assertEquals(dto, result);
        verify(trainingService, never()).createScenariosFromAnalysis(any());
    }

    @Test
    void analyzeGame_throws_whenGameMissing() {
        UUID gameId = UUID.randomUUID();
        when(gameService.getGame(gameId)).thenReturn(Optional.empty());

        assertThrows(GameNotFoundException.class, () -> service.analyzeGame(gameId));
    }
}
