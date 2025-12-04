package ru.chessinsight.infrastructure.persistence.jpa.mapper;

import org.springframework.stereotype.Component;
import ru.chessinsight.application.game.analysis.model.MoveCategory;
import ru.chessinsight.domain.game.model.GameMove;
import ru.chessinsight.domain.game.model.GameMoveAnalysis;
import ru.chessinsight.infrastructure.persistence.jpa.model.GameMoveEntity;

@Component
public class GameMoveMapper implements EntityMapper<GameMove, GameMoveEntity> {
    @Override
    public GameMove toDomain(GameMoveEntity e) {
        if (e == null) return null;
        return new GameMove(
                e.getId(),
                e.getPlyIndex(),
                e.getSan(),
                e.getUci(),
                e.getPositionFEN(),
                e.getCommentBefore(),
                e.getCommentAfter(),
                analysisFromEntity(e)
        );
    }

    @Override
    public GameMoveEntity toEntity(GameMove d) {
        if (d == null) return null;
        var e = new GameMoveEntity();
        e.setId(d.getId());
        e.setPlyIndex(d.getPlyIndex());
        e.setSan(d.getSan());
        e.setUci(d.getUci());
        e.setPositionFEN(d.getPositionFEN());
        e.setCommentBefore(d.getCommentBefore());
        e.setCommentAfter(d.getCommentAfter());
        if (d.getAnalysis() != null) {
            e.setAnalysisBestUci(d.getAnalysis().bestUCI());
            e.setAnalysisCategory(d.getAnalysis().category() != null ? d.getAnalysis().category().name() : null);
            e.setAnalysisCpLoss(d.getAnalysis().cpLoss());
            e.setAnalysisEvalCp(d.getAnalysis().evalCp());
            e.setAnalysisMateScore(d.getAnalysis().mateScore());
        }
        return e;
    }

    private GameMoveAnalysis analysisFromEntity(GameMoveEntity e) {
        if (e.getAnalysisEvalCp() == null) return null;
        return new GameMoveAnalysis(
                e.getAnalysisEvalCp(),
                e.getAnalysisMateScore(),
                e.getAnalysisBestUci(),
                e.getAnalysisCpLoss(),
                (e.getAnalysisCategory() != null) ? MoveCategory.valueOf(e.getAnalysisCategory()) : null
        );
    }
}
