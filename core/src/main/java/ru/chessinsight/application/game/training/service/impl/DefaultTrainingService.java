package ru.chessinsight.application.game.training.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.chessinsight.application.game.analysis.engine.ChessEngine;
import ru.chessinsight.application.game.analysis.engine.dto.EngineAnalysisRequest;
import ru.chessinsight.application.game.analysis.engine.dto.EnginePositionAnalysis;
import ru.chessinsight.application.game.dto.GameAnalysisDTO;
import ru.chessinsight.application.game.dto.MoveAnalysisDTO;
import ru.chessinsight.application.game.training.dto.TrainingMoveRequest;
import ru.chessinsight.application.game.training.dto.TrainingMoveResponse;
import ru.chessinsight.application.game.training.service.TrainingService;
import ru.chessinsight.application.game.training.service.exception.ScenarioCreationException;
import ru.chessinsight.domain.chess.move.notation.service.UciNotationService;
import ru.chessinsight.domain.chess.move.service.MoveMaker;
import ru.chessinsight.domain.chess.position.model.Position;
import ru.chessinsight.domain.common.pagination.Page;
import ru.chessinsight.domain.common.pagination.PageParams;
import ru.chessinsight.domain.game.training.model.TrainingScenario;
import ru.chessinsight.domain.game.training.repository.TrainingScenarioRepository;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DefaultTrainingService implements TrainingService {

    private final TrainingScenarioRepository scenarioRepository;
    private final UciNotationService uciNotationService;
    private final ChessEngine engine;

    private static final int DEFAULT_DEPTH = 8;
    private static final int DEFAULT_MULTIPV = 3;
    private static final int DEFAULT_PV_LIMIT = 5;

    public DefaultTrainingService(TrainingScenarioRepository scenarioRepository,
                                  ChessEngine engine) {
        this.scenarioRepository = scenarioRepository;
        this.uciNotationService = new UciNotationService();
        this.engine = engine;
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
        } catch (Exception ignored) {}

        return s;
    }

    @Override
    public List<TrainingScenario> getUserScenarios(UUID userId, Boolean completed) {
        return scenarioRepository.findAllByCompletionForUser(userId, completed);
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
        TrainingScenario s = scenarioRepository.findOneById(cmd.scenarioId())
                .orElseThrow(() -> new ScenarioCreationException("Scenario not found"));

        if (s.getUserId() != null && !cmd.isDemo() && !s.getUserId().equals(userId)) {
            throw new ScenarioCreationException("Scenario does not belong to user");
        }

        if (s.isCompleted()) {
            return new TrainingMoveResponse(
                    TrainingMoveResponse.Status.COMPLETED,
                    "Scenario already completed.",
                    null, null, cmd.cursor(), true, null, null
            );
        }

        final List<String> pv = splitMovesUci(s.getPvUci());
        if (pv.isEmpty()) {
            return new TrainingMoveResponse(TrainingMoveResponse.Status.INCORRECT,
                    "Scenario has no PV configured.",
                    null, null, cmd.cursor(), false, null, null);
        }

        int cursor = sanitizeCursor(cmd.cursor(), pv.size());

        if (cursor >= pv.size()) {
            s.setCompleted(true);
            scenarioRepository.save(s);
            return new TrainingMoveResponse(
                    TrainingMoveResponse.Status.COMPLETED,
                    "Scenario completed.",
                    null, null, pv.size(), true, null, null
            );
        }

        var pos = Position.fromFEN(s.getPositionFEN());
        var prevMoves = pv.subList(0, cursor);
        for (var move : prevMoves) {
            pos = MoveMaker.apply(pos, uciNotationService.uciToMove(move, pos));
        }

        final String userMoveUci = cmd.moveUCI();

        final String expectedMove = pv.get(cursor);
        if (expectedMove.equals(userMoveUci)) {
            cursor += 1;

            String opponent = null;
            if (cursor < pv.size()) {
                opponent = pv.get(cursor);
                cursor += 1;
            }

            boolean nowCompleted = (cursor >= pv.size());
            if (nowCompleted && !cmd.isDemo()) {
                s.setCompleted(true);
                scenarioRepository.save(s);
            }

            return new TrainingMoveResponse(
                    nowCompleted ? TrainingMoveResponse.Status.COMPLETED : TrainingMoveResponse.Status.CONTINUE,
                    nowCompleted
                            ? "Correct! You’ve reached the end of the line."
                            : "Correct! Opponent replies — your move again.",
                    userMoveUci,
                    opponent,
                    cursor,
                    nowCompleted && !cmd.isDemo(),
                    null, null
            );
        }
        final String fenAfterMistake = MoveMaker.apply(pos, uciNotationService.uciToMove(userMoveUci, pos)).toFEN();
        EnginePositionAnalysis refutation = engine.analyzePosition(
                new EngineAnalysisRequest(
                        fenAfterMistake,
                        DEFAULT_DEPTH,
                        null, null,
                        1,
                        DEFAULT_PV_LIMIT
                )
        );

        List<String> hintSan = (refutation != null && refutation.pvSan() != null) ? refutation.pvSan() : List.of();
        List<String> hintUci = (refutation != null && refutation.pvUci() != null) ? refutation.pvUci() : List.of();

        return new TrainingMoveResponse(
                TrainingMoveResponse.Status.INCORRECT,
                "That move doesn’t match the training line. Here’s a sample refutation:",
                null, null,
                cmd.cursor(),
                false,
                hintSan, hintUci
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
