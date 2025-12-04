package ru.chessinsight.application.game.analysis.service;

import ru.chessinsight.application.game.analysis.model.MoveCategory;
import ru.chessinsight.application.game.dto.MoveAnalysisDTO;

public interface MoveClassificationService {
    MoveCategory classifyMove(MoveAnalysisDTO moveDTO);
}
