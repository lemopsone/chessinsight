package ru.chessinsight.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.chessinsight.application.game.dto.GameMetadataDTO;
import ru.chessinsight.application.game.dto.GameMetadataPatchDTO;
import ru.chessinsight.application.game.dto.GameSearchCriteria;
import ru.chessinsight.application.game.exception.GameNotFoundException;
import ru.chessinsight.application.game.service.GameService;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.model.GameResult;
import ru.chessinsight.domain.game.repository.GameRepository;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.user.repository.UserRepository;
import ru.chessinsight.testdata.GameBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class GameServiceIT extends AbstractIntegrationTest {

    @Autowired
    private GameService gameService;
    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void findUserGames_filtersByCriteria() {
        User user = createUser();
        User other = createUser();

        Game g1 = saveGame(user.getId(), GameResult.WHITE_WIN, LocalDate.of(2024, 1, 1));
        saveGame(user.getId(), GameResult.DRAW, LocalDate.of(2024, 1, 2));
        saveGame(other.getId(), GameResult.WHITE_WIN, LocalDate.of(2024, 1, 3));

        GameSearchCriteria criteria = new GameSearchCriteria(GameResult.WHITE_WIN, null, null, null);
        var page = gameService.findUserGames(user.getId(), criteria);

        List<Game> content = page.content();
        assertEquals(1, content.size());
        assertEquals(g1.getId(), content.get(0).getId());
    }

    @Test
    void updateGameMetadata_updatesFields() {
        User user = createUser();
        Game game = saveGame(user.getId(), GameResult.DRAW, LocalDate.of(2024, 1, 1));

        GameMetadataDTO dto = new GameMetadataDTO(
                "NewEvent",
                "NewSite",
                LocalDate.of(2024, 2, 2),
                "2",
                "White2",
                "Black2",
                GameResult.WHITE_WIN
        );

        Game updated = gameService.updateGameMetadata(game.getId(), dto);

        assertEquals("NewEvent", updated.getEvent());
        assertEquals("NewSite", updated.getSite());
        assertEquals(LocalDate.of(2024, 2, 2), updated.getDate());
        assertEquals("2", updated.getRound());
        assertEquals("White2", updated.getWhiteName());
        assertEquals("Black2", updated.getBlackName());
        assertEquals(GameResult.WHITE_WIN, updated.getResult());

        Game fromDb = gameRepository.findOneById(game.getId()).orElseThrow();
        assertEquals("NewEvent", fromDb.getEvent());
        assertEquals("NewSite", fromDb.getSite());
    }

    @Test
    void updateGameMetadata_throws_whenMissing() {
        GameMetadataDTO dto = new GameMetadataDTO(
                "Event",
                "Site",
                LocalDate.of(2024, 1, 1),
                "1",
                "W",
                "B",
                GameResult.DRAW
        );

        assertThrows(GameNotFoundException.class,
                () -> gameService.updateGameMetadata(UUID.randomUUID(), dto));
    }

    @Test
    void patchGameMetadata_updatesSelectedFields() {
        User user = createUser();
        Game game = saveGame(user.getId(), GameResult.DRAW, LocalDate.of(2024, 1, 1));

        GameMetadataPatchDTO patch = new GameMetadataPatchDTO(
                "PatchedEvent",
                null,
                null,
                "5",
                null
        );

        Game updated = gameService.patchGameMetadata(game.getId(), patch);

        assertEquals("PatchedEvent", updated.getEvent());
        assertEquals("Site", updated.getSite());
        assertEquals("5", updated.getRound());
    }

    @Test
    void deleteGame_removesGame() {
        User user = createUser();
        Game game = saveGame(user.getId(), GameResult.DRAW, LocalDate.of(2024, 1, 1));

        gameService.deleteGame(game.getId());

        assertTrue(gameRepository.findOneById(game.getId()).isEmpty());
    }

    @Test
    void deleteGame_throws_whenMissing() {
        assertThrows(GameNotFoundException.class, () -> gameService.deleteGame(UUID.randomUUID()));
    }

    private User createUser() {
        String login = "user_" + UUID.randomUUID();
        User user = new User();
        user.setLogin(login);
        user.setEmail(login + "@example.com");
        user.setPasswordHash("pass");
        return userRepository.save(user);
    }

    private Game saveGame(UUID userId, GameResult result, LocalDate date) {
        Game game = GameBuilder.game()
                .withUserId(userId)
                .withEvent("Event")
                .withSite("Site")
                .withDate(date)
                .withRound("1")
                .withWhiteName("White")
                .withBlackName("Black")
                .withResult(result)
                .build();
        return gameRepository.save(game);
    }
}
