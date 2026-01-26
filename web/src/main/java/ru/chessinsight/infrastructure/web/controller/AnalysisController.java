package ru.chessinsight.infrastructure.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import ru.chessinsight.application.game.analysis.service.AnalysisService;
import ru.chessinsight.application.game.dto.MoveAnalysisDTO;
import ru.chessinsight.application.game.dto.MoveDTO;
import ru.chessinsight.infrastructure.web.api.AnalysisApi;
import ru.chessinsight.infrastructure.web.mapper.AnalysisApiMapper;

@RestController
public class AnalysisController implements AnalysisApi, ApiV1Controller {

    private final AnalysisService analysisService;
    private final AnalysisApiMapper analysisApiMapper;

    public AnalysisController(AnalysisService analysisService, AnalysisApiMapper analysisApiMapper) {
        this.analysisService = analysisService;
        this.analysisApiMapper = analysisApiMapper;
    }

    @Override
    @PostMapping("/analysis/move")
    public ResponseEntity<ru.chessinsight.infrastructure.web.dto.MoveAnalysisDTO> analyzeMove(
            @Valid @RequestBody ru.chessinsight.infrastructure.web.dto.MoveDTO body
    ) {
        MoveDTO appMove = analysisApiMapper.toAppMove(body);
        MoveAnalysisDTO result = analysisService.analyzeMove(appMove);
        return ResponseEntity.ok(analysisApiMapper.toApiMoveAnalysis(result));
    }
}
