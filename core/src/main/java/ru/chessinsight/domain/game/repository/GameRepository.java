package ru.chessinsight.domain.game.repository;

import ru.chessinsight.domain.game.model.Game;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameRepository {
    Game save(Game game);
    Optional<Game> findOneById(UUID id);
    List<Game> findAllByUserId(UUID userId);
    List<Game> findAllByPgn(String pgn);
}
