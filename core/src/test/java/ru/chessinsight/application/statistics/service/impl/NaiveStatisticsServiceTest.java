package ru.chessinsight.application.statistics.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.chessinsight.application.common.logger.service.Logger;
import ru.chessinsight.application.game.analysis.service.AnalysisService;
import ru.chessinsight.application.game.dto.GameAnalysisDTO;
import ru.chessinsight.application.game.dto.MoveAnalysisDTO;
import ru.chessinsight.application.game.dto.MoveDTO;
import ru.chessinsight.application.statistics.exception.UserNotFoundException;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.repository.GameRepository;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.user.model.UserStatistics;
import ru.chessinsight.domain.user.repository.UserRepository;
import ru.chessinsight.testdata.GameBuilder;
import ru.chessinsight.testdata.UserBuilder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class NaiveStatisticsServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private AnalysisService analysisService;
    @Mock
    private Logger logger;

    private NaiveStatisticsService service;

    @BeforeEach
    void setUp() {
        service = new NaiveStatisticsService(userRepository, gameRepository, analysisService, logger);
    }

    @Test
    void getUserStatistics_returnsAverages() {
        UUID userId = UUID.randomUUID();
        User user = UserBuilder.user().withId(userId).build();
        Game game = GameBuilder.game().withUserId(userId).build();
        when(userRepository.findOneById(userId)).thenReturn(Optional.of(user));
        when(gameRepository.findAllByUserId(userId)).thenReturn(List.of(game));

        MoveAnalysisDTO ma = new MoveAnalysisDTO("fen", new MoveDTO(1L, "fen", "e4", "e2e4"), 0.0, 0.0, null);
        GameAnalysisDTO dto = new GameAnalysisDTO(game.getId(), userId, 0.0, 0.0, List.of(ma), List.of(ma), List.of(), List.of(), List.of());
        when(analysisService.analyzeGame(game)).thenReturn(dto);

        UserStatistics stats = service.getUserStatistics(userId);

        assertEquals(1.0, stats.accuracy());
        assertEquals(1.0, stats.accuracyWhite());
        assertEquals(1.0, stats.accuracyBlack());
    }

    @Test
    void getUserStatistics_throws_whenUserMissing() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findOneById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.getUserStatistics(userId));
        verifyNoInteractions(gameRepository);
    }
}