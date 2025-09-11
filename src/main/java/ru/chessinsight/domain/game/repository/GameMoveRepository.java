package ru.chessinsight.domain.game.repository;

import ru.chessinsight.domain.game.model.GameMove;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameMoveRepository {
    Optional<GameMove> save(GameMove move);
    List<GameMove> findAllGameMoves(UUID gameId);
}
