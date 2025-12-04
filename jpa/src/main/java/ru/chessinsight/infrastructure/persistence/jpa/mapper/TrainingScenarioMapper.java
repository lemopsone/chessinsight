package ru.chessinsight.infrastructure.persistence.jpa.mapper;

import org.springframework.stereotype.Component;
import ru.chessinsight.domain.game.training.model.TrainingScenario;
import ru.chessinsight.infrastructure.persistence.jpa.model.TrainingScenarioEntity;

@Component
public class TrainingScenarioMapper implements EntityMapper<TrainingScenario, TrainingScenarioEntity> {
    @Override
    public TrainingScenario toDomain(TrainingScenarioEntity e) {
        if (e == null) return null;
        var d = new TrainingScenario();
        d.setId(e.getId());
        d.setUserId(e.getUserId());
        d.setGameId(e.getGameId());
        d.setPositionFEN(e.getPositionFEN());
        d.setPvSan(e.getPvSan());
        d.setPvUci(e.getPvUci());
        d.setPrompt(e.getPrompt());
        d.setCompleted(e.isCompleted());
        d.setCompletedAt(e.getCompletedAt());
        return d;
    }

    @Override
    public TrainingScenarioEntity toEntity(TrainingScenario d) {
        if (d == null) return null;
        var e = new TrainingScenarioEntity();
        e.setId(d.getId());
        e.setGameId(d.getGameId());
        e.setUserId(d.getUserId());
        e.setPositionFEN(d.getPositionFEN());
        e.setPvSan(d.getPvSan());
        e.setPvUci(d.getPvUci());
        e.setPrompt(d.getPrompt());
        e.setCompleted(d.isCompleted());
        e.setCompletedAt(d.getCompletedAt());
        return e;
    }
}
