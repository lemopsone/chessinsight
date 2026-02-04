package ru.chessinsight.application.game.analysis.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.chessinsight.application.common.logger.service.Logger;
import ru.chessinsight.application.game.analysis.engine.ChessEngine;
import ru.chessinsight.application.game.analysis.engine.dto.EngineMoveAnalysis;
import ru.chessinsight.application.game.analysis.engine.dto.EngineMoveRequest;
import ru.chessinsight.application.game.analysis.model.MoveCategory;
import ru.chessinsight.application.game.analysis.service.AccuracyCalculationService;
import ru.chessinsight.application.game.analysis.service.MoveClassificationService;
import ru.chessinsight.application.game.dto.GameAnalysisDTO;
import ru.chessinsight.application.game.dto.MoveAnalysisDTO;
import ru.chessinsight.application.game.dto.MoveDTO;
import ru.chessinsight.domain.chess.position.model.Position;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.model.GameAnalysis;
import ru.chessinsight.domain.game.model.GameMove;
import ru.chessinsight.domain.game.model.GameMoveAnalysis;
import ru.chessinsight.domain.game.repository.GameRepository;
import ru.chessinsight.testdata.GameAnalysisBuilder;
import ru.chessinsight.testdata.GameBuilder;
import ru.chessinsight.testdata.GameMoveBuilder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultAnalysisServiceTest {
    @Mock
    private ChessEngine engine;
    @Mock
    private AccuracyCalculationService accuracy;
    @Mock
    private MoveClassificationService classifier;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private Logger logger;

    private DefaultAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new DefaultAnalysisService(engine, accuracy, classifier, gameRepository, logger);
    }

    @Test
    void analyzeGame_computesAndSaves_whenNoAnalysis() {
        String fen = Position.initial().toFEN();
        GameMove move = GameMoveBuilder.move().withPlyIndex(1).withSan("e4").withUci("e2e4").withPositionFEN(fen).build();
        Game game = GameBuilder.game().withId(UUID.randomUUID()).withMoves(Set.of(move)).withAnalysis(null).build();

        when(engine.analyzeMove(any(EngineMoveRequest.class))).thenReturn(
                new EngineMoveAnalysis(fen, "e2e4", "e4", 20, null, "e2e4", "e4", 10, List.of("e2e4"), List.of("e4"))
        );
        when(classifier.classifyMove(any(MoveAnalysisDTO.class))).thenReturn(MoveCategory.GOOD_MOVE);
        when(accuracy.moveWeight(anyLong())).thenReturn(1.0);
        when(accuracy.movePenalty(any(MoveCategory.class), anyLong())).thenReturn(0.0);
        when(accuracy.calculateAccuracy(anyDouble(), anyDouble())).thenReturn(99.0);
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameAnalysisDTO result = service.analyzeGame(game);

        assertEquals(1, result.goodMoves().size());
        assertEquals(99.0, result.accuracyWhite());
        verify(gameRepository).save(game);
    }

    @Test
    void analyzeGame_returnsCached_whenAnalysisPresent() {
        GameAnalysis analysis = GameAnalysisBuilder.analysis().build();
        GameMoveAnalysis moveAnalysis = new GameMoveAnalysis(0.2, null, "e2e4", 0.1, MoveCategory.BEST_MOVE);
        String fen = Position.initial().toFEN();
        GameMove move = GameMoveBuilder.move().withPlyIndex(1).withSan("e4").withUci("e2e4").withPositionFEN(fen).withAnalysis(moveAnalysis).build();
        Game game = GameBuilder.game().withId(UUID.randomUUID()).withAnalysis(analysis).withMoves(Set.of(move)).build();

        GameAnalysisDTO result = service.analyzeGame(game);

        assertEquals(1, result.bestMoves().size());
        verifyNoInteractions(engine);
    }

    @Test
    void analyzeMove_usesUciWhenProvided() {
        String fen = Position.initial().toFEN();
        MoveDTO move = new MoveDTO(1L, fen, "e4", "e2e4");
        when(engine.analyzeMove(any(EngineMoveRequest.class))).thenReturn(
                new EngineMoveAnalysis(fen, "e2e4", "e4", 10, null, "e2e4", "e4", 5, List.of("e2e4"), List.of("e4"))
        );

        MoveAnalysisDTO result = service.analyzeMove(move);

        assertEquals(fen, result.positionFEN());
        assertEquals("e2e4", result.bestMove().moveUCI());
    }

    @Test
    void analyzeMove_usesSanWhenUciMissing() {
        String fen = Position.initial().toFEN();
        MoveDTO move = new MoveDTO(1L, fen, "e4", null);
        when(engine.analyzeMove(any(EngineMoveRequest.class))).thenReturn(
                new EngineMoveAnalysis(fen, "e2e4", "e4", 10, null, "e2e4", "e4", 5, List.of("e2e4"), List.of("e4"))
        );

        MoveAnalysisDTO result = service.analyzeMove(move);

        assertEquals(fen, result.positionFEN());
        assertEquals("e2e4", result.bestMove().moveUCI());
    }
}
