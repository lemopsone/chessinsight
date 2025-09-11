package ru.chessinsight.domain.game.repository;

import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.model.GameMove;

import java.util.List;
import java.util.Optional;

public interface GameMoveRepository {
    Optional<GameMove> save(GameMove move);
    List<GameMove> findAllGameMoves(Game game);
}
