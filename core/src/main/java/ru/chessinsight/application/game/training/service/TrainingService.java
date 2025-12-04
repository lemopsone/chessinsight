package ru.chessinsight.application.game.training.service;

import org.springframework.stereotype.Service;
import ru.chessinsight.application.game.dto.GameAnalysisDTO;
import ru.chessinsight.application.game.training.dto.TrainingMoveRequest;
import ru.chessinsight.application.game.training.dto.TrainingMoveResponse;
import ru.chessinsight.domain.game.training.model.TrainingScenario;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public interface TrainingService {
    TrainingMoveResponse submitMove(UUID userId, TrainingMoveRequest request);
    List<TrainingScenario> createScenariosFromAnalysis(GameAnalysisDTO dto);
    List<TrainingScenario> getNewUserScenarios(UUID userId);
    Optional<TrainingScenario> getScenarioById(UUID scenarioId);
}
