package ru.chessinsight.application.game.training.service;

import org.springframework.stereotype.Service;
import ru.chessinsight.application.game.dto.GameAnalysisDTO;
import ru.chessinsight.application.game.training.dto.TrainingMoveRequest;
import ru.chessinsight.application.game.training.dto.TrainingMoveResponse;
import ru.chessinsight.domain.common.pagination.Page;
import ru.chessinsight.domain.common.pagination.PageParams;
import ru.chessinsight.domain.game.training.model.TrainingScenario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public interface TrainingService {
    TrainingMoveResponse submitMove(UUID userId, TrainingMoveRequest request);
    List<TrainingScenario> createScenariosFromAnalysis(GameAnalysisDTO dto);
    List<TrainingScenario> getUserScenarios(UUID userId, Boolean completed);
    Page<TrainingScenario> getUserScenarios(UUID userId, Boolean completed, PageParams params);
    Optional<TrainingScenario> getScenarioById(UUID scenarioId);
}
