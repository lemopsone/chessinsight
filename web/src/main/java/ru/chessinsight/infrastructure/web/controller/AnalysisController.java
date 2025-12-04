package ru.chessinsight.infrastructure.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.chessinsight.application.game.analysis.service.AnalysisService;
import ru.chessinsight.application.game.dto.GameAnalysisDTO;
import ru.chessinsight.application.game.dto.MoveAnalysisDTO;
import ru.chessinsight.application.game.dto.MoveDTO;
import ru.chessinsight.application.game.service.GameService;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.infrastructure.web.api.AnalysisApi;
import ru.chessinsight.infrastructure.web.mapper.AnalysisApiMapper;

import java.util.UUID;

@RestController
public class AnalysisController implements AnalysisApi, ApiV1Controller {

    private final AnalysisService analysisService;
    private final GameService gameService;
    private final AnalysisApiMapper analysisApiMapper;

    public AnalysisController(AnalysisService analysisService, GameService gameService, AnalysisApiMapper analysisApiMapper) {
        this.analysisService = analysisService;
        this.gameService = gameService;
        this.analysisApiMapper = analysisApiMapper;
    }

    @PostMapping("/analysis/move")
    public ResponseEntity<ru.chessinsight.infrastructure.web.dto.MoveAnalysisDTO> analyzeMove(
            @RequestBody ru.chessinsight.infrastructure.web.dto.MoveDTO body
    ) {
        MoveDTO appMove = analysisApiMapper.toAppMove(body);
        MoveAnalysisDTO result = analysisService.analyzeMove(appMove);
        return ResponseEntity.ok(analysisApiMapper.toApiMoveAnalysis(result));
    }

    @PostMapping("/games/{gameId}/analysis")
    public ResponseEntity<ru.chessinsight.infrastructure.web.dto.GameAnalysisDTO> analyzeGame(
            @PathVariable UUID gameId
    ) {
        Game game = gameService.getGame(gameId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Game not found"
                ));
        GameAnalysisDTO dto = analysisService.analyzeGame(game);
        return ResponseEntity.ok(analysisApiMapper.toApiGameAnalysis(dto));
    }
}
