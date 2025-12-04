package ru.chessinsight.application.game.analysis.service.impl;

import org.springframework.stereotype.Service;
import ru.chessinsight.application.game.analysis.model.MoveCategory;
import ru.chessinsight.application.game.analysis.service.MoveClassificationService;
import ru.chessinsight.application.game.dto.MoveAnalysisDTO;

@Service
public class SimpleMoveClassificationService implements MoveClassificationService {
    private static final double BEST_MOVE_THRESHOLD = 0.1;
    private static final double GOOD_MOVE_THRESHOLD = 0.5;
    private static final double INACCURACY_THRESHOLD = 1;
    private static final double MISTAKE_THRESHOLD = 3;

    @Override
    public MoveCategory classifyMove(MoveAnalysisDTO moveDTO) {
        if (moveDTO.mateScore() != null) {
            return MoveCategory.FORCED_MATE;
        }
        double delta = moveDTO.bestMoveEval() - moveDTO.playerMoveEval();
        if (delta <= BEST_MOVE_THRESHOLD) {
            return MoveCategory.BEST_MOVE;
        } else if (delta <= GOOD_MOVE_THRESHOLD) {
            return MoveCategory.GOOD_MOVE;
        } else if (delta <= INACCURACY_THRESHOLD) {
            return MoveCategory.INACCURACY;
        } else if (delta <= MISTAKE_THRESHOLD) {
            return MoveCategory.MISTAKE;
        } else {
            return MoveCategory.BLUNDER;
        }
    }
}
