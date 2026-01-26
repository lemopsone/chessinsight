package ru.chessinsight.application.game.analysis.service.impl;

import org.springframework.stereotype.Service;
import ru.chessinsight.application.game.analysis.service.AnalysisService;
import ru.chessinsight.application.game.analysis.service.GameAnalysisWorkflowService;
import ru.chessinsight.application.game.dto.GameAnalysisDTO;
import ru.chessinsight.application.game.exception.GameNotFoundException;
import ru.chessinsight.application.game.service.GameService;
import ru.chessinsight.application.game.training.service.TrainingService;
import ru.chessinsight.domain.game.model.Game;

import java.util.UUID;

@Service
public class DefaultGameAnalysisWorkflowService implements GameAnalysisWorkflowService {

    private final GameService gameService;
    private final AnalysisService analysisService;
    private final TrainingService trainingService;

    public DefaultGameAnalysisWorkflowService(GameService gameService,
                                              AnalysisService analysisService,
                                              TrainingService trainingService) {
        this.gameService = gameService;
        this.analysisService = analysisService;
        this.trainingService = trainingService;
    }

    @Override
    public GameAnalysisDTO analyzeGame(UUID gameId) {
        Game game = gameService.getGame(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found"));
        boolean hadAnalysis = game.getAnalysis() != null;
        GameAnalysisDTO dto = analysisService.analyzeGame(game);
        if (!hadAnalysis) {
            trainingService.createScenariosFromAnalysis(dto);
        }
        return dto;
    }
}
