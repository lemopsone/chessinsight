package ru.chessinsight.application.game.training.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.chessinsight.application.common.logger.service.Logger;
import ru.chessinsight.application.game.analysis.engine.ChessEngine;
import ru.chessinsight.application.game.analysis.engine.dto.EngineAnalysisRequest;
import ru.chessinsight.application.game.analysis.engine.dto.EnginePositionAnalysis;
import ru.chessinsight.application.game.dto.GameAnalysisDTO;
import ru.chessinsight.application.game.dto.MoveAnalysisDTO;
import ru.chessinsight.application.game.dto.MoveDTO;
import ru.chessinsight.application.game.training.dto.TrainingMoveRequest;
import ru.chessinsight.application.game.training.dto.TrainingMoveResponse;
import ru.chessinsight.application.game.training.service.exception.ScenarioAccessException;
import ru.chessinsight.application.game.training.service.exception.ScenarioCreationException;
import ru.chessinsight.application.game.training.service.exception.ScenarioNotFoundException;
import ru.chessinsight.domain.chess.position.model.Position;
import ru.chessinsight.domain.common.pagination.Page;
import ru.chessinsight.domain.common.pagination.PageParams;
import ru.chessinsight.domain.game.training.model.TrainingScenario;
import ru.chessinsight.domain.game.training.repository.TrainingScenarioRepository;
import ru.chessinsight.testdata.TrainingScenarioBuilder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import io.qameta.allure.Tag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DefaultTrainingServiceTest {
    @Mock
    private TrainingScenarioRepository scenarioRepository;
    @Mock
    private ChessEngine engine;
    @Mock
    private Logger logger;

    private DefaultTrainingService service;

    @BeforeEach
    void setUp() {
        service = new DefaultTrainingService(scenarioRepository, engine, logger);
    }

    @Test
    void createScenariosFromAnalysis_createsUniqueScenarios() {
        UUID userId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        String fen = Position.initial().toFEN();
        MoveAnalysisDTO ma1 = new MoveAnalysisDTO(fen, new MoveDTO(1L, fen, "e4", "e2e4"), 0.2, 0.1, null);
        MoveAnalysisDTO ma2 = new MoveAnalysisDTO(fen, new MoveDTO(2L, fen, "e4", "e2e4"), 0.2, 0.1, null);
        GameAnalysisDTO dto = new GameAnalysisDTO(gameId, userId, 90.0, 88.0, List.of(), List.of(), List.of(), List.of(ma1, ma2), List.of());

        EnginePositionAnalysis engineAns = new EnginePositionAnalysis(fen, 8, 8, "e2e4", "e4", List.of("e2e4"), List.of("e4"), 0, null, List.of());
        when(engine.analyzePosition(any(EngineAnalysisRequest.class))).thenReturn(engineAns);
        when(scenarioRepository.save(any(TrainingScenario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<TrainingScenario> result = service.createScenariosFromAnalysis(dto);

        assertEquals(1, result.size());
        assertEquals(fen, result.get(0).getPositionFEN());
        assertEquals("e4", result.get(0).getPvSan());
    }

    @Test
    void createScenariosFromAnalysis_throws_whenDtoNull() {
        assertThrows(ScenarioCreationException.class, () -> service.createScenariosFromAnalysis(null));
    }

    @Test
    void getUserScenarios_returnsList() {
        UUID userId = UUID.randomUUID();
        TrainingScenario scenario = TrainingScenarioBuilder.scenario().withUserId(userId).build();
        when(scenarioRepository.findAllByCompletionForUser(userId, true)).thenReturn(List.of(scenario));

        List<TrainingScenario> result = service.getUserScenarios(userId, true);

        assertEquals(1, result.size());
        assertEquals(scenario, result.get(0));
    }

    @Test
    void getUserScenarios_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        when(scenarioRepository.findAllByCompletionForUser(userId, null)).thenReturn(List.of());

        List<TrainingScenario> result = service.getUserScenarios(userId, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void getUserScenariosPage_returnsPage() {
        UUID userId = UUID.randomUUID();
        Page<TrainingScenario> page = new Page<>(List.of(), 0, 20, 0);
        when(scenarioRepository.findAllByCompletionForUser(userId, null, new PageParams(0, 20))).thenReturn(page);

        Page<TrainingScenario> result = service.getUserScenariosPage(userId, null);

        assertEquals(page, result);
    }

    @Test
    void getUserScenariosPage_returnsEmptyPage() {
        UUID userId = UUID.randomUUID();
        Page<TrainingScenario> page = new Page<>(List.of(), 0, 20, 0);
        when(scenarioRepository.findAllByCompletionForUser(userId, true, new PageParams(0, 20))).thenReturn(page);

        Page<TrainingScenario> result = service.getUserScenariosPage(userId, true);

        assertTrue(result.content().isEmpty());
    }

    @Test
    void getUserScenarios_withParams_returnsPage() {
        UUID userId = UUID.randomUUID();
        PageParams params = new PageParams(1, 5);
        Page<TrainingScenario> page = new Page<>(List.of(), 1, 5, 0);
        when(scenarioRepository.findAllByCompletionForUser(userId, false, params)).thenReturn(page);

        Page<TrainingScenario> result = service.getUserScenarios(userId, false, params);

        assertEquals(page, result);
    }

    @Test
    void getUserScenarios_withParams_returnsEmptyPage() {
        UUID userId = UUID.randomUUID();
        PageParams params = new PageParams(0, 3);
        Page<TrainingScenario> page = new Page<>(List.of(), 0, 3, 0);
        when(scenarioRepository.findAllByCompletionForUser(userId, null, params)).thenReturn(page);

        Page<TrainingScenario> result = service.getUserScenarios(userId, null, params);

        assertTrue(result.content().isEmpty());
    }

    @Test
    void getScenarioById_returnsOptional() {
        UUID id = UUID.randomUUID();
        TrainingScenario scenario = TrainingScenarioBuilder.scenario().withId(id).build();
        when(scenarioRepository.findOneById(id)).thenReturn(Optional.of(scenario));

        Optional<TrainingScenario> result = service.getScenarioById(id);

        assertTrue(result.isPresent());
        assertEquals(scenario, result.get());
    }

    @Test
    void getScenarioById_returnsEmpty() {
        UUID id = UUID.randomUUID();
        when(scenarioRepository.findOneById(id)).thenReturn(Optional.empty());

        Optional<TrainingScenario> result = service.getScenarioById(id);

        assertTrue(result.isEmpty());
    }

    @Test
    void submitMove_throws_whenScenarioMissing() {
        UUID scenarioId = UUID.randomUUID();
        when(scenarioRepository.findOneById(scenarioId)).thenReturn(Optional.empty());

        assertThrows(ScenarioNotFoundException.class,
                () -> service.submitMove(UUID.randomUUID(), new TrainingMoveRequest(scenarioId, 0, "e2e4", false)));
    }

    @Test
    void submitMove_throws_whenAccessForbidden() {
        UUID scenarioId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        TrainingScenario scenario = TrainingScenarioBuilder.scenario().withId(scenarioId).withUserId(ownerId).build();
        when(scenarioRepository.findOneById(scenarioId)).thenReturn(Optional.of(scenario));

        assertThrows(ScenarioAccessException.class,
                () -> service.submitMove(UUID.randomUUID(), new TrainingMoveRequest(scenarioId, 0, "e2e4", false)));
    }

    @Test
    void submitMove_returnsCompleted_whenAlreadyCompleted() {
        UUID scenarioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TrainingScenario scenario = TrainingScenarioBuilder.scenario()
                .withId(scenarioId)
                .withUserId(userId)
                .withCompleted(true)
                .build();
        when(scenarioRepository.findOneById(scenarioId)).thenReturn(Optional.of(scenario));

        TrainingMoveResponse result = service.submitMove(userId, new TrainingMoveRequest(scenarioId, 0, "e2e4", false));

        assertEquals(TrainingMoveResponse.Status.COMPLETED, result.status());
    }

    @Test
    void submitMove_returnsIncorrect_whenPvMissing() {
        UUID scenarioId = UUID.randomUUID();
        TrainingScenario scenario = TrainingScenarioBuilder.scenario().withId(scenarioId).withPvUci(null).build();
        when(scenarioRepository.findOneById(scenarioId)).thenReturn(Optional.of(scenario));

        TrainingMoveResponse result = service.submitMove(UUID.randomUUID(), new TrainingMoveRequest(scenarioId, 0, "e2e4", true));

        assertEquals(TrainingMoveResponse.Status.INCORRECT, result.status());
    }

    @Test
    void submitMove_returnsContinue_whenCorrectMove() {
        UUID scenarioId = UUID.randomUUID();
        TrainingScenario scenario = TrainingScenarioBuilder.scenario()
                .withId(scenarioId)
                .withPvUci("e2e4 e7e5 g1f3")
                .build();
        when(scenarioRepository.findOneById(scenarioId)).thenReturn(Optional.of(scenario));

        TrainingMoveResponse result = service.submitMove(UUID.randomUUID(), new TrainingMoveRequest(scenarioId, 0, "e2e4", true));

        assertEquals(TrainingMoveResponse.Status.CONTINUE, result.status());
        assertEquals(2, result.nextCursor());
    }

    @Test
    void submitMove_returnsIncorrect_whenMoveWrong() {
        UUID scenarioId = UUID.randomUUID();
        String fen = Position.initial().toFEN();
        TrainingScenario scenario = TrainingScenarioBuilder.scenario().withId(scenarioId).withPositionFEN(fen).withPvUci("e2e4").build();
        when(scenarioRepository.findOneById(scenarioId)).thenReturn(Optional.of(scenario));
        EnginePositionAnalysis refutation = new EnginePositionAnalysis(fen, 8, 8, "d2d4", "d4", List.of("d2d4"), List.of("d4"), 0, null, List.of());
        when(engine.analyzePosition(any(EngineAnalysisRequest.class))).thenReturn(refutation);

        TrainingMoveResponse result = service.submitMove(UUID.randomUUID(), new TrainingMoveRequest(scenarioId, 0, "d2d4", true));

        assertEquals(TrainingMoveResponse.Status.INCORRECT, result.status());
        assertFalse(result.hintPvUci().isEmpty());
    }
}