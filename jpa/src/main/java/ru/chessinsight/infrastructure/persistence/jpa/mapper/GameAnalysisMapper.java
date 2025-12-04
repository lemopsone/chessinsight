package ru.chessinsight.infrastructure.persistence.jpa.mapper;

import org.springframework.stereotype.Component;
import ru.chessinsight.domain.game.model.GameAnalysis;
import ru.chessinsight.infrastructure.persistence.jpa.model.GameAnalysisEntity;

@Component
public class GameAnalysisMapper implements EntityMapper<GameAnalysis, GameAnalysisEntity> {
    @Override
    public GameAnalysis toDomain(GameAnalysisEntity e) {
        if (e == null) return null;
        var d = new GameAnalysis();
        d.setAccuracyWhite(e.getAccuracyWhite());
        d.setAccuracyBlack(e.getAccuracyBlack());
        d.setMistakes(e.getMistakes());
        d.setBlunders(e.getBlunders());
        d.setInaccuracies(e.getInaccuracies());
        d.setAnalyzedAt(e.getAnalyzedAt());

        return d;
    }

    @Override
    public GameAnalysisEntity toEntity(GameAnalysis d) {
        if (d == null) return null;
        var e = new GameAnalysisEntity();
        e.setAccuracyWhite(d.getAccuracyWhite());
        e.setAccuracyBlack(d.getAccuracyBlack());
        e.setInaccuracies(d.getInaccuracies());
        e.setMistakes(d.getMistakes());
        e.setBlunders(d.getBlunders());
        e.setAnalyzedAt(d.getAnalyzedAt());
        return e;
    }
}
