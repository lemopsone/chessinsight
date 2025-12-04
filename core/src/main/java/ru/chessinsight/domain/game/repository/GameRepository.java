package ru.chessinsight.domain.game.repository;

import ru.chessinsight.domain.common.pagination.Page;
import ru.chessinsight.domain.common.pagination.PageParams;
import ru.chessinsight.domain.game.model.Game;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameRepository {
    Game save(Game game);
    Optional<Game> findOneById(UUID id);
    List<Game> findAllByUserId(UUID userId);
    Page<Game> findAllByUserId(UUID userId, PageParams params);
    List<Game> findAllByPgn(String pgn);
    void delete(Game game);
}
