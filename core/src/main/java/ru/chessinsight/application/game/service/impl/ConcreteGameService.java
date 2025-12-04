package ru.chessinsight.application.game.service.impl;

import org.springframework.stereotype.Service;
import ru.chessinsight.application.game.dto.GameMetadataDTO;
import ru.chessinsight.application.game.exception.GameNotFoundException;
import ru.chessinsight.application.game.service.GameService;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.repository.GameRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConcreteGameService implements GameService {
    private final GameRepository gameRepository;
    private static final UUID DEMO_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    public ConcreteGameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Override
    public List<Game> findUserGames(UUID userId) {
        return gameRepository.findAllByUserId(userId);
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
        Game game = getGame(gameId).orElseThrow();
        game.setEvent(metadataDTO.event());
        game.setSite(metadataDTO.site());
        game.setDate(metadataDTO.date());
        game.setRound(metadataDTO.round());
        if (metadataDTO.result() != null) {
            game.setResult(metadataDTO.result());
        }
        game.setWhiteName(metadataDTO.whiteName());
        game.setBlackName(metadataDTO.blackName());
        return gameRepository.save(game);
    }

    @Override
    public void deleteGame(UUID gameId) throws GameNotFoundException {
        Game game = getGame(gameId).orElseThrow();
        gameRepository.delete(game);
    }
}
