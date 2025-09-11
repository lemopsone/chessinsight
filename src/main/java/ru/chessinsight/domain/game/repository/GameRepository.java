package ru.chessinsight.domain.game.repository;

import ru.chessinsight.domain.game.model.Game;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameRepository {
    Optional<Game> save(Game game);
    Optional<Game> findOneById(UUID id);
    Optional<Game> findOneByPGN(String pgn);
    List<Game> findAllByUserId(UUID userId);
}
