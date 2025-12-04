package ru.chessinsight.application.game.analysis.service;

import org.springframework.stereotype.Service;
import ru.chessinsight.application.game.dto.GameAnalysisDTO;
import ru.chessinsight.application.game.dto.MoveAnalysisDTO;
import ru.chessinsight.application.game.dto.MoveDTO;
import ru.chessinsight.domain.game.model.Game;

@Service
public interface AnalysisService {
    GameAnalysisDTO analyzeGame(Game game);
    MoveAnalysisDTO analyzeMove(MoveDTO move);
}
