package ru.chessinsight.infrastructure.web.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.chessinsight.application.auth.service.AuthService;
import ru.chessinsight.application.game.dto.GameMetadataDTO;
import ru.chessinsight.application.game.exception.GameNotFoundException;
import ru.chessinsight.application.game.service.GameImportService;
import ru.chessinsight.application.game.service.GameService;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.infrastructure.web.api.GamesApi;
import ru.chessinsight.infrastructure.web.dto.GameCreateFromPgnRequest;
import ru.chessinsight.infrastructure.web.dto.GameDTO;
import ru.chessinsight.infrastructure.web.dto.GameResult;
import ru.chessinsight.infrastructure.web.dto.PageResponseGameDTO;
import ru.chessinsight.infrastructure.web.dto.PatchGameRequest;
import ru.chessinsight.infrastructure.web.dto.ReplaceGameRequest;
import ru.chessinsight.infrastructure.web.mapper.GameApiMapper;

import java.time.LocalDate;
import java.util.UUID;

@RestController
public class GameController implements GamesApi, ApiV1Controller {

    private final AuthService authService;
    private final GameService gameService;
    private final GameImportService gameImportService;
    private final GameApiMapper gameApiMapper;

    public GameController(AuthService authService,
                          GameService gameService,
                          GameImportService gameImportService,
                          GameApiMapper gameApiMapper) {
        this.authService = authService;
        this.gameService = gameService;
        this.gameImportService = gameImportService;
        this.gameApiMapper = gameApiMapper;
    }

    @Override
    @GetMapping("/users/me/games")
    public ResponseEntity<PageResponseGameDTO> listMyGames(
            @RequestParam(required = false) GameResult result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Boolean analyzed
    ) {
        UUID userId = authService.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Not authenticated"));

        PageResponseGameDTO response = gameApiMapper.toPageResponse(
                gameService.findUserGames(
                        userId,
                        gameApiMapper.toSearchCriteria(result, dateFrom, dateTo, analyzed)
                )
        );

        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/games")
    public ResponseEntity<GameDTO> importGameFromPgn(
            @Valid @RequestBody GameCreateFromPgnRequest body
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
                .orElseThrow(() -> new GameNotFoundException("Game not found"));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(gameApiMapper.toGameDto(game));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/games")
    public ResponseEntity<PageResponseGameDTO> listUserGamesAdmin(
            @PathVariable UUID userId,
            @RequestParam(required = false) GameResult result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Boolean analyzed
    ) {
        PageResponseGameDTO response = gameApiMapper.toPageResponse(
                gameService.findUserGames(
                        userId,
                        gameApiMapper.toSearchCriteria(result, dateFrom, dateTo, analyzed)
                )
        );
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/games/{gameId}")
    public ResponseEntity<GameDTO> getGame(@PathVariable UUID gameId) {
        Game game = gameService.getGame(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found"));
        return ResponseEntity.ok(gameApiMapper.toGameDto(game));
    }

    @Override
    @PutMapping("/games/{gameId}")
    public ResponseEntity<GameDTO> replaceGame(
            @PathVariable UUID gameId,
            @Valid @RequestBody ReplaceGameRequest body
    ) {
        GameMetadataDTO meta = gameApiMapper.toMetaDto(body);
        Game saved = gameService.updateGameMetadata(gameId, meta);
        return ResponseEntity.ok(gameApiMapper.toGameDto(saved));
    }

    @Override
    @PatchMapping("/games/{gameId}")
    public ResponseEntity<GameDTO> patchGame(
            @PathVariable UUID gameId,
            @Valid @RequestBody PatchGameRequest body
    ) {
        var patch = gameApiMapper.toPatchDto(body);
        Game saved = gameService.patchGameMetadata(gameId, patch);
        return ResponseEntity.ok(gameApiMapper.toGameDto(saved));
    }

    @Override
    @DeleteMapping("/games/{gameId}")
    public ResponseEntity<Void> deleteGame(@PathVariable UUID gameId) {
        gameService.deleteGame(gameId);
        return ResponseEntity.noContent().build();
    }
}
