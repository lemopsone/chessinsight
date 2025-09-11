package ru.chessinsight.domain.game.repository;

import org.jmolecules.ddd.annotation.Repository;
import ru.chessinsight.domain.game.model.Game;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameRepository {
    Optional<Game> save(Game game);
    Optional<Game> findOneById(UUID id);
    Optional<Game> findOneByPGN(String pgn);
    List<Game> findAllByUserId(UUID userId);
}
