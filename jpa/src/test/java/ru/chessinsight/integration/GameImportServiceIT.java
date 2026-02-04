package ru.chessinsight.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.chessinsight.application.exception.ApplicationException;
import ru.chessinsight.application.game.service.GameImportService;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.model.GameResult;
import ru.chessinsight.domain.game.repository.GameRepository;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.user.repository.UserRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class GameImportServiceIT extends AbstractIntegrationTest {

    @Autowired
    private GameImportService gameImportService;
    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void importFromPgn_persistsGameAndMoves() {
        User user = createUser();
        String pgn = "[Event \"Casual\"]\n" +
                "[Site \"Here\"]\n" +
                "[Date \"2024.01.01\"]\n" +
                "[Round \"1\"]\n" +
                "[White \"W\"]\n" +
                "[Black \"B\"]\n" +
                "[Result \"1-0\"]\n\n" +
                "1. e4 e5 2. Nf3 Nc6 1-0";

        UUID gameId = gameImportService.importFromPgn(user.getId(), pgn, null, "1-0");

        Game game = gameRepository.findOneById(gameId).orElseThrow();
        assertEquals(GameResult.WHITE_WIN, game.getResult());
        assertFalse(game.getMoves().isEmpty());
    }

    @Test
    void importFromMoves_persistsGame() {
        User user = createUser();

        UUID gameId = gameImportService.importFromMoves(user.getId(), "uci", "e2e4 e7e5", null, "*");

        Game game = gameRepository.findOneById(gameId).orElseThrow();
        assertEquals(GameResult.UNFINISHED, game.getResult());
        assertEquals(2, game.getMoves().size());
    }

    @Test
    void importFromMoves_throws_onUnsupportedFormat() {
        User user = createUser();

        assertThrows(ApplicationException.class,
                () -> gameImportService.importFromMoves(user.getId(), "pgn", "1. e4 e5", null, "*"));
    }

    private User createUser() {
        String login = "user_" + UUID.randomUUID();
        User user = new User();
        user.setLogin(login);
        user.setEmail(login + "@example.com");
        user.setPasswordHash("pass");
        return userRepository.save(user);
    }
}