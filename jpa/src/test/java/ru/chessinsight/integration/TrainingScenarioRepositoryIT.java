package ru.chessinsight.integration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.chessinsight.domain.game.model.GameResult;
import ru.chessinsight.domain.game.repository.GameRepository;
import ru.chessinsight.domain.game.training.model.TrainingScenario;
import ru.chessinsight.domain.game.training.repository.TrainingScenarioRepository;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import io.qameta.allure.Tag;

@Tag("integration")
public class TrainingScenarioRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private TrainingScenarioRepository repo;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private GameRepository gameRepo;

    @Test
    void save_find_and_filter_completed() {
        var user = new User();
        user.setLogin("tom"); user.setEmail("t@t"); user.setPasswordHash("y");
        user = userRepo.save(user);

        var game = new Game();
        game.setUserId(user.getId());
        game.setDate(LocalDate.now());
        game.setResult(GameResult.UNFINISHED);
        game = gameRepo.save(game);

        TrainingScenario ts = new TrainingScenario();
        ts.setUserId(user.getId());
        ts.setGameId(game.getId());
        ts.setPositionFEN("K7/8/8/8/3kK3/8/8/7k w - - 0 1");
        ts.setPrompt("Find the best move");
        ts.setCompleted(false);

        var saved = repo.save(ts);
        Assertions.assertNotNull(saved.getId());

        var forUser = repo.findAllByUserId(user.getId());
        Assertions.assertEquals(1, forUser.size());

        var uncompleted = repo.findAllByCompletionForUser(user.getId(), false);
        Assertions.assertEquals(1, uncompleted.size());

        saved.setCompleted(true);
        saved.setCompletedAt(OffsetDateTime.now());
        var saved2 = repo.save(saved);
        Assertions.assertTrue(saved2.isCompleted());
    }
}