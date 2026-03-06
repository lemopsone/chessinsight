package ru.chessinsight.application.game.training.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.chessinsight.application.common.logger.service.Logger;
import ru.chessinsight.application.game.analysis.engine.ChessEngine;
import ru.chessinsight.application.game.analysis.engine.dto.EngineAnalysisRequest;
import ru.chessinsight.application.game.analysis.engine.dto.EnginePositionAnalysis;
import ru.chessinsight.application.game.dto.GameAnalysisDTO;
import ru.chessinsight.application.game.dto.MoveAnalysisDTO;
import ru.chessinsight.application.game.training.dto.TrainingMoveRequest;
import ru.chessinsight.application.game.training.dto.TrainingMoveResponse;
import ru.chessinsight.application.game.training.service.TrainingService;
import ru.chessinsight.application.game.training.service.exception.ScenarioAccessException;
import ru.chessinsight.application.game.training.service.exception.ScenarioCreationException;
import ru.chessinsight.application.game.training.service.exception.ScenarioNotFoundException;
import ru.chessinsight.domain.chess.move.notation.service.UciNotationService;
import ru.chessinsight.domain.chess.move.service.MoveMaker;
import ru.chessinsight.domain.chess.position.model.Position;
import ru.chessinsight.domain.common.pagination.Page;
import ru.chessinsight.domain.common.pagination.PageParams;
import ru.chessinsight.domain.game.training.model.TrainingScenario;
import ru.chessinsight.domain.game.training.repository.TrainingScenarioRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DefaultTrainingService implements TrainingService {

    private final TrainingScenarioRepository scenarioRepository;
    private final UciNotationService uciNotationService;
    private final ChessEngine engine;
    private final Logger logger;

    private static final int DEFAULT_DEPTH = 8;
    private static final int DEFAULT_MULTIPV = 3;
    private static final int DEFAULT_PV_LIMIT = 5;

    public DefaultTrainingService(TrainingScenarioRepository scenarioRepository,
                                  ChessEngine engine,
                                  Logger logger) {
        this.scenarioRepository = scenarioRepository;
        this.uciNotationService = new UciNotationService();
        this.engine = engine;
        this.logger = logger;
    }

    @Override
    public List<TrainingScenario> createScenariosFromAnalysis(GameAnalysisDTO dto) {
        if (dto == null) throw new ScenarioCreationException("GameAnalysisDTO is null");

        Stream<MoveAnalysisDTO> trainingMoves = Stream.of(
                safe(dto.mistakes()),
                safe(dto.blunders())
        ).flatMap(Collection::stream);

        Set<String> seenFens = new HashSet<>();

        List<TrainingScenario> created = new ArrayList<>();
        trainingMoves
                .filter(Objects::nonNull)
                .filter(ma -> ma.positionFEN() != null && !ma.positionFEN().isBlank())
                .filter(ma -> seenFens.add(ma.positionFEN()))
                .map(ma -> createScenarioFromMoveAnalysis(dto, ma))
                .map(scenarioRepository::save)
                .forEach(created::add);

        logger.info("training.scenarios.created userId=" + dto.userId()
                + " gameId=" + dto.gameId()
                + " count=" + created.size());
        return created;
    }

    private List<MoveAnalysisDTO> safe(List<MoveAnalysisDTO> list) {
        return list == null ? List.of() : list;
    }

    private TrainingScenario createScenarioFromMoveAnalysis(GameAnalysisDTO game, MoveAnalysisDTO moveAnalysis) {
        TrainingScenario s = new TrainingScenario();
        s.setUserId(game.userId());
        s.setGameId(game.gameId());
        s.setPositionFEN(moveAnalysis.positionFEN());
        s.setCompleted(false);

        try {
            EnginePositionAnalysis engineAns = engine.analyzePosition(
                    new EngineAnalysisRequest(
                            moveAnalysis.positionFEN(),
                            DEFAULT_DEPTH,
                            null,
                            null,
                            DEFAULT_MULTIPV,
                            DEFAULT_PV_LIMIT
                    )
            );

            if (engineAns != null) {
                List<String> pvSan = engineAns.pvSan();
                List<String> pvUci = engineAns.pvUci();

                if (pvSan != null && !pvSan.isEmpty()) {
                    s.setPvSan(String.join(" ", pvSan));
                }
                if (pvUci != null && !pvUci.isEmpty()) {
                    s.setPvUci(String.join(" ", pvUci));
                }

                String prompt = "Find the best continuation for the side to move.";
                s.setPrompt(prompt);
            }
        } catch (Exception ex) {
            logger.warning("training.scenario.pv.failed gameId=" + game.gameId()
                    + " reason=" + ex.getClass().getSimpleName());
        }

        return s;
    }

    @Override
    public List<TrainingScenario> getUserScenarios(UUID userId, Boolean completed) {
        return scenarioRepository.findAllByCompletionForUser(userId, completed);
    }

    @Override
    public Page<TrainingScenario> getUserScenariosPage(UUID userId, Boolean completed) {
        return getUserScenarios(userId, completed, new PageParams(0, 20));
    }

    @Override
    public Page<TrainingScenario> getUserScenarios(UUID userId, Boolean completed, PageParams params) {
        return scenarioRepository.findAllByCompletionForUser(userId, completed, params);
    }

    @Override
    public Optional<TrainingScenario> getScenarioById(UUID scenarioId) {
        return scenarioRepository.findOneById(scenarioId);
    }

    @Override
    @Transactional
    public TrainingMoveResponse submitMove(UUID userId, TrainingMoveRequest cmd) {
        TrainingScenario scenario = requireScenario(cmd.scenarioId());
        validateAccess(userId, cmd, scenario);
        if (scenario.isCompleted()) {
            logger.info("training.submit.completed scenarioId=" + cmd.scenarioId());
            return completedAlreadyResponse(cmd.cursor());
        }

        List<String> pv = splitMovesUci(scenario.getPvUci());
        if (pv.isEmpty()) {
            logger.warning("training.submit.missing_pv scenarioId=" + cmd.scenarioId());
            return missingPvResponse(cmd.cursor());
        }

        int cursor = sanitizeCursor(cmd.cursor(), pv.size());
        if (cursor >= pv.size()) {
            return completeScenario(scenario, cmd.scenarioId(), pv.size());
        }

        Position position = positionAtCursor(scenario.getPositionFEN(), pv, cursor);
        String userMoveUci = cmd.moveUCI();
        String expectedMove = pv.get(cursor);
        if (expectedMove.equals(userMoveUci)) {
            return processCorrectMove(scenario, cmd, pv, cursor, userMoveUci);
        }
        return processIncorrectMove(cmd, position, userMoveUci);
    }

    private TrainingScenario requireScenario(UUID scenarioId) {
        Optional<TrainingScenario> scenario = scenarioRepository.findOneById(scenarioId);
        if (scenario.isPresent()) {
            return scenario.get();
        }
        logger.warning("training.submit.not_found scenarioId=" + scenarioId);
        throw new ScenarioNotFoundException("Scenario not found");
    }

    private void validateAccess(UUID userId, TrainingMoveRequest cmd, TrainingScenario scenario) {
        if (scenario.getUserId() != null && !cmd.isDemo() && !scenario.getUserId().equals(userId)) {
            logger.warning("training.submit.forbidden scenarioId=" + cmd.scenarioId()
                    + " userId=" + userId);
            throw new ScenarioAccessException("Scenario does not belong to user");
        }
    }

    private static TrainingMoveResponse completedAlreadyResponse(int cursor) {
        return new TrainingMoveResponse(
                TrainingMoveResponse.Status.COMPLETED,
                "Scenario already completed.",
                null,
                null,
                cursor,
                true,
                null,
                null
        );
    }

    private static TrainingMoveResponse missingPvResponse(int cursor) {
        return new TrainingMoveResponse(
                TrainingMoveResponse.Status.INCORRECT,
                "Scenario has no PV configured.",
                null,
                null,
                cursor,
                false,
                null,
                null
        );
    }

    private TrainingMoveResponse completeScenario(TrainingScenario scenario, UUID scenarioId, int cursor) {
        scenario.setCompleted(true);
        scenario.setCompletedAt(OffsetDateTime.now());
        scenarioRepository.save(scenario);
        logger.info("training.submit.completed scenarioId=" + scenarioId);
        return new TrainingMoveResponse(
                TrainingMoveResponse.Status.COMPLETED,
                "Scenario completed.",
                null,
                null,
                cursor,
                true,
                null,
                null
        );
    }

    private Position positionAtCursor(String initialFen, List<String> pv, int cursor) {
        Position position = Position.fromFEN(initialFen);
        for (String move : pv.subList(0, cursor)) {
            position = MoveMaker.apply(position, uciNotationService.uciToMove(move, position));
        }
        return position;
    }

    private TrainingMoveResponse processCorrectMove(
            TrainingScenario scenario,
            TrainingMoveRequest cmd,
            List<String> pv,
            int cursor,
            String userMoveUci
    ) {
        int nextCursor = cursor + 1;
        String opponentMove = null;
        if (nextCursor < pv.size()) {
            opponentMove = pv.get(nextCursor);
            nextCursor += 1;
        }

        boolean nowCompleted = nextCursor >= pv.size();
        if (nowCompleted && !cmd.isDemo()) {
            scenario.setCompleted(true);
            scenario.setCompletedAt(OffsetDateTime.now());
            scenarioRepository.save(scenario);
            logger.info("training.submit.completed scenarioId=" + cmd.scenarioId());
        }
        return new TrainingMoveResponse(
                nowCompleted ? TrainingMoveResponse.Status.COMPLETED : TrainingMoveResponse.Status.CONTINUE,
                nowCompleted
                        ? "Correct! You’ve reached the end of the line."
                        : "Correct! Opponent replies — your move again.",
                userMoveUci,
                opponentMove,
                nextCursor,
                nowCompleted && !cmd.isDemo(),
                null,
                null
        );
    }

    private TrainingMoveResponse processIncorrectMove(TrainingMoveRequest cmd, Position position, String userMoveUci) {
        String fenAfterMistake = MoveMaker.apply(position, uciNotationService.uciToMove(userMoveUci, position)).toFEN();
        EnginePositionAnalysis refutation = engine.analyzePosition(
                new EngineAnalysisRequest(
                        fenAfterMistake,
                        DEFAULT_DEPTH,
                        null,
                        null,
                        1,
                        DEFAULT_PV_LIMIT
                )
        );

        List<String> hintSan = refutation != null && refutation.pvSan() != null ? refutation.pvSan() : List.of();
        List<String> hintUci = refutation != null && refutation.pvUci() != null ? refutation.pvUci() : List.of();
        return new TrainingMoveResponse(
                TrainingMoveResponse.Status.INCORRECT,
                "That move doesn’t match the training line. Here’s a sample refutation:",
                null,
                null,
                cmd.cursor(),
                false,
                hintSan,
                hintUci
        );
    }


    private static List<String> splitMovesUci(String spaceSeparated) {
        if (spaceSeparated == null || spaceSeparated.isBlank()) return List.of();
        return Arrays.stream(spaceSeparated.trim().split("\\s+"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    private static int sanitizeCursor(int cursor, int pvSize) {
        if (cursor < 0) return 0;
        if (cursor > pvSize) return pvSize;
        return cursor;
    }
}
