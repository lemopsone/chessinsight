package ru.chessinsight.infrastructure.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.chessinsight.application.auth.service.AuthService;
import ru.chessinsight.application.game.dto.GameMetadataDTO;
import ru.chessinsight.application.game.exception.GameNotFoundException;
import ru.chessinsight.application.game.service.GameImportService;
import ru.chessinsight.application.game.service.GameService;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.infrastructure.web.api.GamesApi;
import ru.chessinsight.infrastructure.web.dto.GameCreateFromPgnRequest;
import ru.chessinsight.infrastructure.web.dto.GameDTO;
import ru.chessinsight.infrastructure.web.mapper.GameApiMapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class GameController implements GamesApi, ApiV1Controller {

    private final AuthService authService;
    private final GameService gameService;
    private final GameImportService gameImportService;
    private final GameApiMapper gameApiMapper;

    public GameController(AuthService authService, GameService gameService, GameImportService gameImportService, GameApiMapper gameApiMapper) {
        this.authService = authService;
        this.gameService = gameService;
        this.gameImportService = gameImportService;
        this.gameApiMapper = gameApiMapper;
    }

    @GetMapping("/games")
    public ResponseEntity<List<GameDTO>> listMyGames(
            @RequestParam(required = false) ru.chessinsight.infrastructure.web.dto.GameResult result
    ) {
        UUID userId = authService.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Not authenticated"));

        List<Game> games = gameService.findUserGames(userId);
        if (result != null) {
            var domainResult = gameApiMapper.toDomainResult(result);
            games = games.stream()
                    .filter(g -> domainResult.equals(g.getResult()))
                    .toList();
        }

        List<GameDTO> dtoList = games.stream()
                .map(gameApiMapper::toGameDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    @PostMapping("/games")
    public ResponseEntity<GameDTO> createGameFromPgn(
            @RequestBody GameCreateFromPgnRequest body
    ) {
        UUID userId = authService.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Not authenticated"));

        UUID gameId = gameImportService.importFromPgn(
                userId,
                body.getPgn(),
                body.getStartFen(),
                body.getResultTag()
        );

        Game game = gameService.getGame(gameId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Game not found"
                ));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(gameApiMapper.toGameDto(game));
    }

    @GetMapping("/games/{gameId}")
    public ResponseEntity<GameDTO> getGame(@PathVariable UUID gameId) {
        Game game = gameService.getGame(gameId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Game not found"
                ));
        return ResponseEntity.ok(gameApiMapper.toGameDto(game));
    }

    @PutMapping("/games/{gameId}")
    public ResponseEntity<GameDTO> replaceGame(
            @PathVariable UUID gameId,
            @RequestBody GameDTO body
    ) {
        try {
            GameMetadataDTO meta = gameApiMapper.toMetaDto(body);
            Game saved = gameService.updateGameMetadata(gameId, meta);
            return ResponseEntity.ok(gameApiMapper.toGameDto(saved));
        } catch (GameNotFoundException _) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");
        }
    }

    @DeleteMapping("/games/{gameId}")
    public ResponseEntity<Void> deleteGame(@PathVariable UUID gameId) {
        try {
            gameService.deleteGame(gameId);
        } catch (GameNotFoundException _) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");
        }
        return ResponseEntity.ok().build();
    }
}
