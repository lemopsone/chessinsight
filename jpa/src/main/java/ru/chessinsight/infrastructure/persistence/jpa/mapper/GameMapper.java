package ru.chessinsight.infrastructure.persistence.jpa.mapper;

import org.springframework.stereotype.Component;
import ru.chessinsight.application.game.analysis.model.MoveCategory;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.model.GameMove;
import ru.chessinsight.domain.game.model.GameMoveAnalysis;
import ru.chessinsight.domain.game.model.GameResult;
import ru.chessinsight.infrastructure.persistence.jpa.model.GameEntity;
import ru.chessinsight.infrastructure.persistence.jpa.model.GameMoveEntity;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

@Component
public class GameMapper implements EntityMapper<Game, GameEntity> {
    private final GameAnalysisMapper analysisMapper;

    public GameMapper(GameAnalysisMapper analysisMapper) {
        this.analysisMapper = analysisMapper;
    }

    @Override
    public Game toDomain(GameEntity e) {
        if (e == null) return null;
        var d = new Game();
        d.setId(e.getId());
        d.setUserId(e.getUserId());
        d.setSite(e.getSite());
        d.setPgn(e.getPgn());
        d.setDate(e.getDate());
        d.setEvent(e.getEvent());
        if (e.getResult() != null) {
            d.setResult(GameResult.valueOf(e.getResult()));
        }
        d.setRound(e.getRound());
        d.setWhiteName(e.getWhiteName());
        d.setBlackName(e.getBlackName());
        d.setAnalysis(analysisMapper.toDomain(e.getAnalysis()));
        d.setMoves(movesToDomain(e));

        return d;
    }

    @Override
    public GameEntity toEntity(Game d) {
        if (d == null) return null;
        var e = new GameEntity();
        if (d.getId() != null) {
            e.setId(d.getId());
        }
        e.setUserId(d.getUserId());
        e.setSite(d.getSite());
        e.setPgn(d.getPgn());
        e.setDate(d.getDate());
        e.setEvent(d.getEvent());
        e.setResult(d.getResult() != null ? d.getResult().name() : null);
        e.setRound(d.getRound());
        e.setWhiteName(d.getWhiteName());
        e.setBlackName(d.getBlackName());
        e.setAnalysis(analysisMapper.toEntity(d.getAnalysis()));
        e.setMoves(movesToEntity(d));

        return e;
    }

    public Set<GameMove> movesToDomain(GameEntity e) {
        Comparator<GameMove> c = Comparator.comparing(GameMove::getPlyIndex);
        TreeSet<GameMove> moves = new TreeSet<>(c);
        e.getMoves().forEach(m -> moves.add(moveToDomain(m)));
        return moves;
    }

    public Set<GameMoveEntity> movesToEntity(Game d) {
        Comparator<GameMoveEntity> c = Comparator.comparing(GameMoveEntity::getPlyIndex);
        TreeSet<GameMoveEntity> moves = new TreeSet<>(c);
        d.getMoves().forEach(m -> moves.add(moveToEntity(m)));
        return moves;
    }

    public GameMove moveToDomain(GameMoveEntity e) {
        if (e == null) return null;
        return new GameMove(
                e.getId(),
                e.getPlyIndex(),
                e.getSan(),
                e.getUci(),
                e.getPositionFEN(),
                e.getCommentBefore(),
                e.getCommentAfter(),
                moveAnalysisFromEntity(e)
        );
    }

    public GameMoveEntity moveToEntity(GameMove d) {
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

    private GameMoveAnalysis moveAnalysisFromEntity(GameMoveEntity e) {
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
