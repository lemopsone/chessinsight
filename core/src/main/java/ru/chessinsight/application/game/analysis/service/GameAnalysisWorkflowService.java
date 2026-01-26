package ru.chessinsight.application.game.analysis.service;

import org.springframework.stereotype.Service;
import ru.chessinsight.application.game.dto.GameAnalysisDTO;

import java.util.UUID;

@Service
public interface GameAnalysisWorkflowService {
    GameAnalysisDTO analyzeGame(UUID gameId);
}
