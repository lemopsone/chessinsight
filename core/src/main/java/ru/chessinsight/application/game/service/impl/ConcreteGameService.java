package ru.chessinsight.application.game.service.impl;

import org.springframework.stereotype.Service;
import ru.chessinsight.application.common.logger.service.Logger;
import ru.chessinsight.application.game.dto.GameMetadataDTO;
import ru.chessinsight.application.game.dto.GameMetadataPatchDTO;
import ru.chessinsight.application.game.dto.GameSearchCriteria;
import ru.chessinsight.application.game.exception.GameNotFoundException;
import ru.chessinsight.application.game.service.GameService;
import ru.chessinsight.domain.common.pagination.Page;
import ru.chessinsight.domain.common.pagination.PageParams;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.repository.GameRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConcreteGameService implements GameService {
    private final GameRepository gameRepository;
    private final Logger logger;
    private static final UUID DEMO_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    public ConcreteGameService(GameRepository gameRepository, Logger logger) {
        this.gameRepository = gameRepository;
        this.logger = logger;
    }

    @Override
    public List<Game> findUserGames(UUID userId) {
        return gameRepository.findAllByUserId(userId);
    }

    @Override
    public Page<Game> findUserGames(UUID userId, PageParams params) {
        return gameRepository.findAllByUserId(userId, params);
    }

    @Override
    public Page<Game> findUserGames(UUID userId, GameSearchCriteria criteria) {
        return findUserGames(userId, criteria, new PageParams(0, 20));
    }

    @Override
    public Page<Game> findUserGames(UUID userId, GameSearchCriteria criteria, PageParams params) {
        List<Game> filtered = gameRepository.findAllByUserId(userId).stream()
                .filter(game -> criteria == null || criteria.result() == null || criteria.result() == game.getResult())
                .filter(game -> criteria == null || criteria.dateFrom() == null
                        || (game.getDate() != null && !game.getDate().isBefore(criteria.dateFrom())))
                .filter(game -> criteria == null || criteria.dateTo() == null
                        || (game.getDate() != null && !game.getDate().isAfter(criteria.dateTo())))
                .filter(game -> criteria == null || criteria.analyzed() == null
                        || (criteria.analyzed() ? game.getAnalysis() != null : game.getAnalysis() == null))
                .toList();

        int page = params != null ? params.page() : 0;
        int size = params != null ? params.size() : filtered.size();
        int total = filtered.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);

        List<Game> content = filtered.subList(fromIndex, toIndex);
        return new Page<>(content, page, size, total);
    }

    @Override
    public List<Game> findDemoGames() {
        return gameRepository.findAllByUserId(DEMO_USER_ID);
    }

    @Override
    public Optional<Game> getGame(UUID gameId) {
        return gameRepository.findOneById(gameId);
    }

    @Override
    public Game updateGameMetadata(UUID gameId, GameMetadataDTO metadataDTO) throws GameNotFoundException {
        Game game = getGame(gameId).orElseThrow(() -> new GameNotFoundException("Game not found"));
        game.setEvent(metadataDTO.event());
        game.setSite(metadataDTO.site());
        game.setDate(metadataDTO.date());
        game.setRound(metadataDTO.round());
        if (metadataDTO.result() != null) {
            game.setResult(metadataDTO.result());
        }
        game.setWhiteName(metadataDTO.whiteName());
        game.setBlackName(metadataDTO.blackName());
        Game saved = gameRepository.save(game);
        logger.info("game.metadata.updated gameId=" + gameId);
        return saved;
    }

    @Override
    public Game patchGameMetadata(UUID gameId, GameMetadataPatchDTO patchDTO) throws GameNotFoundException {
        Game game = getGame(gameId).orElseThrow(() -> new GameNotFoundException("Game not found"));
        if (patchDTO.event() != null) {
            game.setEvent(patchDTO.event());
        }
        if (patchDTO.site() != null) {
            game.setSite(patchDTO.site());
        }
        if (patchDTO.date() != null) {
            game.setDate(patchDTO.date());
        }
        if (patchDTO.round() != null) {
            game.setRound(patchDTO.round());
        }
        if (patchDTO.result() != null) {
            game.setResult(patchDTO.result());
        }
        Game saved = gameRepository.save(game);
        logger.info("game.metadata.patch gameId=" + gameId);
        return saved;
    }

    @Override
    public void deleteGame(UUID gameId) throws GameNotFoundException {
        Game game = getGame(gameId).orElseThrow(() -> new GameNotFoundException("Game not found"));
        gameRepository.delete(game);
        logger.info("game.deleted gameId=" + gameId);
    }
}
