package ru.chessinsight.infrastructure.web.mapper;

import org.springframework.stereotype.Component;
import ru.chessinsight.application.game.training.dto.TrainingMoveRequest;
import ru.chessinsight.application.game.training.dto.TrainingMoveResponse;
import ru.chessinsight.domain.common.pagination.Page;
import ru.chessinsight.domain.game.training.model.TrainingScenario;
import ru.chessinsight.infrastructure.web.dto.TrainingMoveStatus;
import ru.chessinsight.infrastructure.web.dto.PageResponseTrainingScenarioDTO;
import ru.chessinsight.infrastructure.web.dto.TrainingScenarioDTO;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TrainingApiMapper {

    public TrainingScenarioDTO toScenarioDto(TrainingScenario s) {
        if (s == null) {
            return null;
        }
        TrainingScenarioDTO dto = new TrainingScenarioDTO();
        dto.setId(s.getId());
        dto.setUserId(s.getUserId());
        dto.setGameId(s.getGameId());
        dto.setPositionFEN(s.getPositionFEN());
        dto.setPvSan(s.getPvSan());
        dto.setPvUci(s.getPvUci());
        dto.setPrompt(s.getPrompt());
        dto.setCompleted(s.isCompleted());
        dto.setCompletedAt(s.getCompletedAt());
        return dto;
    }

    public List<TrainingScenarioDTO> toScenarioDtoList(List<TrainingScenario> list) {
        return list.stream().map(this::toScenarioDto).collect(Collectors.toList());
    }

    public PageResponseTrainingScenarioDTO toPageResponse(Page<TrainingScenario> page) {
        PageResponseTrainingScenarioDTO response = new PageResponseTrainingScenarioDTO();
        response.setContent(page.content().stream().map(this::toScenarioDto).toList());
        response.setPage(page.page());
        response.setSize(page.size());
        response.setTotalElements(page.totalElements());
        response.setTotalPages(page.totalPages());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());
        return response;
    }

    public TrainingMoveRequest toAppRequest(
            ru.chessinsight.infrastructure.web.dto.TrainingMoveRequest dto,
            UUID userId
    ) {
        boolean isDemo = dto.getIsDemo() != null && dto.getIsDemo();
        return new TrainingMoveRequest(
                dto.getScenarioId(),
                dto.getCursor(),
                dto.getMoveUCI(),
                isDemo
        );
    }

    public TrainingMoveRequest toAppRequest(
            ru.chessinsight.infrastructure.web.dto.TrainingMoveCommandDTO dto,
            UUID scenarioId,
            UUID userId
    ) {
        boolean isDemo = dto.getIsDemo() != null && dto.getIsDemo();
        return new TrainingMoveRequest(
                scenarioId,
                dto.getCursor(),
                dto.getMoveUCI(),
                isDemo
        );
    }

    public ru.chessinsight.infrastructure.web.dto.TrainingMoveResponse toApiResponse(
            TrainingMoveResponse resp
    ) {
        ru.chessinsight.infrastructure.web.dto.TrainingMoveResponse dto =
                new ru.chessinsight.infrastructure.web.dto.TrainingMoveResponse();
        dto.setStatus(toStatus(resp.status()));
        dto.setMessage(resp.message());
        dto.setAcceptedMoveUci(resp.acceptedMoveUci());
        dto.setOpponentMoveUci(resp.opponentMoveUci());
        dto.setNextCursor(resp.nextCursor());
        dto.setCompleted(resp.completed());
        dto.setHintPvSan(resp.hintPvSan());
        dto.setHintPvUci(resp.hintPvUci());
        return dto;
    }

    private TrainingMoveStatus toStatus(TrainingMoveResponse.Status status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case CONTINUE -> TrainingMoveStatus.CONTINUE;
            case COMPLETED -> TrainingMoveStatus.COMPLETED;
            case INCORRECT -> TrainingMoveStatus.INCORRECT;
        };
    }
}
