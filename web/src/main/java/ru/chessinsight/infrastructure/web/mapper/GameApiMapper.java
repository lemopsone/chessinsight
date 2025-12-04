package ru.chessinsight.infrastructure.web.mapper;

import org.springframework.stereotype.Component;
import ru.chessinsight.application.game.dto.GameMetadataDTO;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.model.GameMove;
import ru.chessinsight.domain.game.model.GameResult;
import ru.chessinsight.infrastructure.web.dto.GameDTO;
import ru.chessinsight.infrastructure.web.dto.GameMoveDTO;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GameApiMapper {

    public GameDTO toGameDto(Game game) {
        if (game == null) {
            return null;
        }
        GameDTO dto = new GameDTO();
        dto.setId(game.getId());
        dto.setUserId(game.getUserId());
        dto.setEvent(game.getEvent());
        dto.setSite(game.getSite());
        dto.setDate(game.getDate());
        dto.setRound(game.getRound());
        dto.setWhiteName(game.getWhiteName());
        dto.setBlackName(game.getBlackName());
        dto.setResult(game.getResult() != null
                ? ru.chessinsight.infrastructure.web.dto.GameResult.valueOf(game.getResult().name())
                : null);
        dto.setPgn(game.getPgn());

        if (game.getMoves() != null) {
            List<GameMoveDTO> moves = game.getMoves().stream()
                    .sorted(Comparator.comparingInt(GameMove::getPlyIndex))
                    .map(this::toMoveDto)
                    .collect(Collectors.toList());
            dto.setMoves(moves);
        }

        return dto;
    }

    public GameMoveDTO toMoveDto(GameMove move) {
        GameMoveDTO dto = new GameMoveDTO();
        dto.setId(move.getId());
        dto.setPlyIndex(move.getPlyIndex());
        dto.setSan(move.getSan());
        dto.setUci(move.getUci());
        dto.setPositionFEN(move.getPositionFEN());
        dto.setCommentBefore(move.getCommentBefore());
        dto.setCommentAfter(move.getCommentAfter());
        return dto;
    }

    public ru.chessinsight.domain.game.model.GameResult toDomainResult(
            ru.chessinsight.infrastructure.web.dto.GameResult result
    ) {
        if (result == null) {
            return null;
        }
        return GameResult.valueOf(result.name());
    }

    public GameMetadataDTO toMetaDto(GameDTO gameDTO) {
        return new GameMetadataDTO(
                gameDTO.getEvent(),
                gameDTO.getSite(),
                gameDTO.getDate(),
                gameDTO.getRound(),
                gameDTO.getWhiteName(),
                gameDTO.getBlackName(),
                toDomainResult(gameDTO.getResult())
        );
    }
}
