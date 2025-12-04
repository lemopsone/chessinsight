package ru.chessinsight.infrastructure.web.mapper;

import org.springframework.stereotype.Component;
import ru.chessinsight.application.game.dto.GameAnalysisDTO;
import ru.chessinsight.application.game.dto.MoveAnalysisDTO;
import ru.chessinsight.application.game.dto.MoveDTO;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AnalysisApiMapper {

    public MoveDTO toAppMove(ru.chessinsight.infrastructure.web.dto.MoveDTO dto) {
        if (dto == null) {
            return null;
        }
        return new MoveDTO(
                dto.getMoveNum(),
                dto.getPositionFEN(),
                dto.getMoveSAN(),
                dto.getMoveUCI()
        );
    }

    public ru.chessinsight.infrastructure.web.dto.MoveDTO toApiMove(MoveDTO dto) {
        if (dto == null) {
            return null;
        }
        ru.chessinsight.infrastructure.web.dto.MoveDTO out =
                new ru.chessinsight.infrastructure.web.dto.MoveDTO();
        out.setMoveNum(dto.moveNum());
        out.setPositionFEN(dto.positionFEN());
        out.setMoveSAN(dto.moveSAN());
        out.setMoveUCI(dto.moveUCI());
        return out;
    }

    public ru.chessinsight.infrastructure.web.dto.MoveAnalysisDTO toApiMoveAnalysis(MoveAnalysisDTO dto) {
        if (dto == null) {
            return null;
        }
        ru.chessinsight.infrastructure.web.dto.MoveAnalysisDTO out =
                new ru.chessinsight.infrastructure.web.dto.MoveAnalysisDTO();
        out.setPositionFEN(dto.positionFEN());
        out.setBestMove(toApiMove(dto.bestMove()));
        out.setBestMoveEval(dto.bestMoveEval());
        out.setPlayerMoveEval(dto.playerMoveEval());
        out.setMateScore(dto.mateScore());
        return out;
    }

    public ru.chessinsight.infrastructure.web.dto.GameAnalysisDTO toApiGameAnalysis(GameAnalysisDTO dto) {
        if (dto == null) {
            return null;
        }
        ru.chessinsight.infrastructure.web.dto.GameAnalysisDTO out =
                new ru.chessinsight.infrastructure.web.dto.GameAnalysisDTO();
        out.setGameId(dto.gameId());
        out.setUserId(dto.userId());
        out.setAccuracyWhite(dto.accuracyWhite());
        out.setAccuracyBlack(dto.accuracyBlack());
        out.setBestMoves(mapMoves(dto.bestMoves()));
        out.setGoodMoves(mapMoves(dto.goodMoves()));
        out.setInaccuracies(mapMoves(dto.inaccuracies()));
        out.setMistakes(mapMoves(dto.mistakes()));
        out.setBlunders(mapMoves(dto.blunders()));
        return out;
    }

    private List<ru.chessinsight.infrastructure.web.dto.MoveAnalysisDTO> mapMoves(List<MoveAnalysisDTO> list) {
        if (list == null) {
            return null;
        }
        return list.stream()
                .map(this::toApiMoveAnalysis)
                .collect(Collectors.toList());
    }
}
