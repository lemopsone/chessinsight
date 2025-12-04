package ru.chessinsight.integration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.chessinsight.domain.game.model.*;
import ru.chessinsight.domain.game.repository.GameRepository;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class GameRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private GameRepository repo;

    @Autowired
    private UserRepository userRepo;

    @Test
    void save_and_find_by_user_and_pgn() {
        var user = new User();
        user.setLogin("bob");
        user.setEmail("b@b");
        user.setPasswordHash("x");
        user = userRepo.save(user);

        Game g = new Game();
        g.setUserId(user.getId());
        g.setEvent("Casual");
        g.setSite("Here");
        g.setDate(LocalDate.of(2025,1,1));
        g.setRound("1");
        g.setWhiteName("W"); g.setBlackName("B");
        g.setResult(GameResult.UNFINISHED);
        g.setPgn("[Event \"Casual\"] 1. e4 e5 *");

        GameMove m1 = new GameMove();
        m1.setPlyIndex(1); m1.setSan("e4"); m1.setUci("e2e4"); g.addMove(m1);

        GameMove m2 = new GameMove();
        m2.setPlyIndex(2); m2.setSan("e5"); m2.setUci("e7e5"); g.addMove(m2);

        GameAnalysis analysis = new GameAnalysis();
        analysis.setAnalyzedAt(OffsetDateTime.now());
        analysis.setAccuracyWhite(100.);
        analysis.setAccuracyBlack(90.);
        analysis.setInaccuracies(1);
        analysis.setMistakes(2);
        analysis.setBlunders(3);
        g.setAnalysis(analysis);

        var saved = repo.save(g);
        Assertions.assertEquals(g.getAnalysis(), analysis);

        Assertions.assertNotNull(saved.getId());

        var byUser = repo.findAllByUserId(user.getId());
        List<UUID> gameIds = byUser.stream().map(Game::getId).toList();
        Assertions.assertTrue(gameIds.contains(saved.getId()));
        Assertions.assertFalse(byUser.isEmpty());

        Assertions.assertFalse(saved.getMoves().isEmpty());

        var byPgn = repo.findAllByPgn(g.getPgn());
        gameIds = byPgn.stream().map(Game::getId).toList();
        Assertions.assertTrue(gameIds.contains(saved.getId()));
    }
}
