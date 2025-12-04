package ru.chessinsight.application.game.service;

import ru.chessinsight.application.game.dto.GameMetadataDTO;
import ru.chessinsight.application.game.exception.GameNotFoundException;
import ru.chessinsight.domain.game.model.Game;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameService {
    List<Game> findUserGames(UUID userId);
    List<Game> findDemoGames();
    Optional<Game> getGame(UUID gameId);
    Game updateGameMetadata(UUID gameId, GameMetadataDTO metadataDTO) throws GameNotFoundException;
    void deleteGame(UUID gameId) throws GameNotFoundException;
}
