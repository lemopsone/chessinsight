package ru.chessinsight.domain.game.repository;

import ru.chessinsight.domain.game.model.GameAnalysis;

import java.util.Optional;
import java.util.UUID;

public interface GameAnalysisRepository {
    Optional<GameAnalysis> save(GameAnalysis analysis);
    Optional<GameAnalysis> findOneForGame(UUID gameId);
}
