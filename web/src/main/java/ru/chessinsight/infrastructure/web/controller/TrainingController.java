package ru.chessinsight.infrastructure.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.chessinsight.application.auth.service.AuthService;
import ru.chessinsight.application.game.training.dto.TrainingMoveRequest;
import ru.chessinsight.application.game.training.dto.TrainingMoveResponse;
import ru.chessinsight.application.game.training.service.TrainingService;
import ru.chessinsight.domain.game.training.model.TrainingScenario;
import ru.chessinsight.infrastructure.web.api.TrainingApi;
import ru.chessinsight.infrastructure.web.dto.TrainingScenarioDTO;
import ru.chessinsight.infrastructure.web.mapper.TrainingApiMapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @GetMapping("/training/scenarios")
    public ResponseEntity<List<TrainingScenarioDTO>> listScenarios(
            @RequestParam(required = false) Boolean completed
    ) {
        UUID userId = authService.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Not authenticated"));

        List<TrainingScenario> scenarios = trainingService.getNewUserScenarios(userId);
        if (completed != null) {
            scenarios = scenarios.stream()
                    .filter(s -> completed.equals(s.isCompleted()))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(trainingApiMapper.toScenarioDtoList(scenarios));
    }

    @GetMapping("/training/scenarios/{scenarioId}")
    public ResponseEntity<TrainingScenarioDTO> getScenario(@PathVariable UUID scenarioId) {
        TrainingScenario scenario = trainingService.getScenarioById(scenarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Scenario not found"
                ));
        return ResponseEntity.ok(trainingApiMapper.toScenarioDto(scenario));
    }

    @PostMapping("/training/move")
    public ResponseEntity<ru.chessinsight.infrastructure.web.dto.TrainingMoveResponse> submitMove(
            @RequestBody ru.chessinsight.infrastructure.web.dto.TrainingMoveRequest body
    ) {
        UUID userId = authService.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Not authenticated"));

        TrainingMoveRequest req = trainingApiMapper.toAppRequest(body, userId);
        TrainingMoveResponse resp = trainingService.submitMove(userId, req);
        return ResponseEntity.ok(trainingApiMapper.toApiResponse(resp));
    }
}
