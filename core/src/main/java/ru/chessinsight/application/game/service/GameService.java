package ru.chessinsight.application.game.service;

import ru.chessinsight.application.game.dto.GameMetadataDTO;
import ru.chessinsight.application.game.dto.GameMetadataPatchDTO;
import ru.chessinsight.application.game.dto.GameSearchCriteria;
import ru.chessinsight.application.game.exception.GameNotFoundException;
import ru.chessinsight.domain.common.pagination.Page;
import ru.chessinsight.domain.common.pagination.PageParams;
import ru.chessinsight.domain.game.model.Game;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameService {
    List<Game> findUserGames(UUID userId);
    Page<Game> findUserGames(UUID userId, PageParams params);
    Page<Game> findUserGames(UUID userId, GameSearchCriteria criteria);
    Page<Game> findUserGames(UUID userId, GameSearchCriteria criteria, PageParams params);
    List<Game> findDemoGames();
    Optional<Game> getGame(UUID gameId);
    Game updateGameMetadata(UUID gameId, GameMetadataDTO metadataDTO) throws GameNotFoundException;
    Game patchGameMetadata(UUID gameId, GameMetadataPatchDTO patchDTO) throws GameNotFoundException;
    void deleteGame(UUID gameId) throws GameNotFoundException;
}
