package ru.chessinsight.application.game.service;

import ru.chessinsight.domain.game.model.Game;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameService {
    List<Game> findUserGames(UUID userId);
    List<Game> findDemoGames();
    Optional<Game> getGame(UUID gameId);
}
