package ru.chessinsight.infrastructure.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
    @GetMapping("/users/me/training-scenarios")
    public ResponseEntity<PageResponseTrainingScenarioDTO> listMyTrainingScenarios(
            @RequestParam(required = false) Boolean completed
    ) {
        UUID userId = authService.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Not authenticated"));

        var scenarios = trainingService.getUserScenariosPage(userId, completed);

        return ResponseEntity.ok(trainingApiMapper.toPageResponse(scenarios));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/training-scenarios")
    public ResponseEntity<PageResponseTrainingScenarioDTO> listUserTrainingScenariosAdmin(
            @PathVariable UUID userId,
            @RequestParam(required = false) Boolean completed
    ) {
        var scenarios = trainingService.getUserScenariosPage(userId, completed);
        return ResponseEntity.ok(trainingApiMapper.toPageResponse(scenarios));
    }

    @Override
    @GetMapping("/training-scenarios/{scenarioId}")
    public ResponseEntity<TrainingScenarioDTO> getTrainingScenario(@PathVariable UUID scenarioId) {
        var scenario = trainingService.getScenarioById(scenarioId)
                .orElseThrow(() -> new ScenarioNotFoundException("Scenario not found"));
        return ResponseEntity.ok(trainingApiMapper.toScenarioDto(scenario));
    }

    @Override
    @PostMapping("/training-scenarios/{scenarioId}/moves")
    public ResponseEntity<ru.chessinsight.infrastructure.web.dto.TrainingMoveResponse> submitTrainingMove(
            @PathVariable UUID scenarioId,
            @Valid @RequestBody ru.chessinsight.infrastructure.web.dto.TrainingMoveCommandDTO body
    ) {
        UUID userId = authService.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Not authenticated"));

        TrainingMoveRequest req = trainingApiMapper.toAppRequest(body, scenarioId, userId);
        TrainingMoveResponse resp = trainingService.submitMove(userId, req);
        return ResponseEntity.ok(trainingApiMapper.toApiResponse(resp));
    }
}
