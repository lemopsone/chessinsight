package ru.chessinsight.infrastructure.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import ru.chessinsight.application.auth.service.AuthService;
import ru.chessinsight.application.game.training.dto.TrainingMoveRequest;
import ru.chessinsight.application.game.training.dto.TrainingMoveResponse;
import ru.chessinsight.application.game.training.service.TrainingService;
import ru.chessinsight.application.game.training.service.exception.ScenarioNotFoundException;
import ru.chessinsight.infrastructure.web.api.TrainingApi;
import ru.chessinsight.infrastructure.web.dto.PageResponseTrainingScenarioDTO;
import ru.chessinsight.infrastructure.web.dto.TrainingScenarioDTO;
import ru.chessinsight.infrastructure.web.mapper.TrainingApiMapper;

import java.util.UUID;

@RestController
public class TrainingController implements TrainingApi, ApiV1Controller {

    private final AuthService authService;
    private final TrainingService trainingService;
    private final TrainingApiMapper trainingApiMapper;

    public TrainingController(AuthService authService, TrainingService trainingService, TrainingApiMapper trainingApiMapper) {
        this.authService = authService;
        this.trainingService = trainingService;
        this.trainingApiMapper = trainingApiMapper;
    }

    @Override
    @GetMapping("/training/scenarios")
    public ResponseEntity<PageResponseTrainingScenarioDTO> listMyTrainingScenarios(
            @RequestParam(required = false) Boolean completed
    ) {
        UUID userId = authService.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Not authenticated"));

        var scenarios = trainingService.getUserScenariosPage(userId, completed);

        return ResponseEntity.ok(trainingApiMapper.toPageResponse(scenarios));
    }

    @Override
    @GetMapping("/training/scenarios/{scenarioId}")
    public ResponseEntity<TrainingScenarioDTO> getTrainingScenario(@PathVariable UUID scenarioId) {
        var scenario = trainingService.getScenarioById(scenarioId)
                .orElseThrow(() -> new ScenarioNotFoundException("Scenario not found"));
        return ResponseEntity.ok(trainingApiMapper.toScenarioDto(scenario));
    }

    @Override
    @PostMapping("/training/move")
    public ResponseEntity<ru.chessinsight.infrastructure.web.dto.TrainingMoveResponse> submitTrainingMove(
            @Valid @RequestBody ru.chessinsight.infrastructure.web.dto.TrainingMoveRequest body
    ) {
        UUID userId = authService.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Not authenticated"));

        TrainingMoveRequest req = trainingApiMapper.toAppRequest(body, userId);
        TrainingMoveResponse resp = trainingService.submitMove(userId, req);
        return ResponseEntity.ok(trainingApiMapper.toApiResponse(resp));
    }
}
