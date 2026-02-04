package ru.chessinsight.application.game.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.chessinsight.application.common.logger.service.Logger;
import ru.chessinsight.application.exception.ApplicationException;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.repository.GameRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConcreteGameImportServiceTest {
    @Mock
    private GameRepository gameRepository;
    @Mock
    private Logger logger;

    private ConcreteGameImportService service;

    @BeforeEach
    void setUp() {
        service = new ConcreteGameImportService(gameRepository, logger);
    }

    @Test
    void importFromPgn_returnsGameId_whenSaved() {
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
            Game g = invocation.getArgument(0);
            g.setId(UUID.randomUUID());
            return g;
        });
        String pgn = """
            [Event "Test"]
            1. e4 e5 2. Nf3 Nc6 *
            """;

        UUID id = service.importFromPgn(UUID.randomUUID(), pgn, null, "1-0");

        assertNotNull(id);
    }

    @Test
    void importFromPgn_returnsNull_whenSaveFails() {
        when(gameRepository.save(any(Game.class))).thenReturn(null);
        String pgn = """
            [Event "Test"]
            1. e4 e5 *
            """;

        UUID id = service.importFromPgn(UUID.randomUUID(), pgn, null, null);

        assertNull(id);
    }

    @Test
    void importFromMoves_uci_returnsGameId() {
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
            Game g = invocation.getArgument(0);
            g.setId(UUID.randomUUID());
            return g;
        });

        UUID id = service.importFromMoves(UUID.randomUUID(), "uci", "e2e4 e7e5", null, "1/2-1/2");

        assertNotNull(id);
    }

    @Test
    void importFromMoves_throws_whenFormatUnknown() {
        assertThrows(ApplicationException.class,
                () -> service.importFromMoves(UUID.randomUUID(), "bad", "e2e4", null, null));
    }
}
