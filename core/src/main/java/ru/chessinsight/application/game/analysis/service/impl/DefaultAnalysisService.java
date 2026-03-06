package ru.chessinsight.application.game.analysis.service.impl;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.chessinsight.application.common.logger.service.Logger;
import ru.chessinsight.application.game.analysis.engine.ChessEngine;
import ru.chessinsight.application.game.analysis.engine.dto.EngineMoveRequest;
import ru.chessinsight.application.game.analysis.service.AccuracyCalculationService;
import ru.chessinsight.application.game.analysis.service.AnalysisService;
import ru.chessinsight.application.game.analysis.service.MoveClassificationService;
import ru.chessinsight.application.game.analysis.model.MoveCategory;
import ru.chessinsight.application.game.dto.GameAnalysisDTO;
import ru.chessinsight.application.game.dto.MoveAnalysisDTO;
import ru.chessinsight.application.game.dto.MoveDTO;
import ru.chessinsight.domain.chess.move.model.Move;
import ru.chessinsight.domain.chess.move.notation.service.SanNotationService;
import ru.chessinsight.domain.chess.move.notation.service.UciNotationService;
import ru.chessinsight.domain.chess.move.service.MoveMaker;
import ru.chessinsight.domain.chess.position.model.Position;
import ru.chessinsight.domain.chess.piece.model.Color;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.model.GameAnalysis;
import ru.chessinsight.domain.game.model.GameMove;
import ru.chessinsight.domain.game.model.GameMoveAnalysis;
import ru.chessinsight.domain.game.repository.GameRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DefaultAnalysisService implements AnalysisService {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(DefaultAnalysisService.class);

    private final ChessEngine engine;
    private final AccuracyCalculationService accuracy;
    private final MoveClassificationService classifier;
    private final GameRepository gameRepository;
    private final SanNotationService san;
    private final UciNotationService uci;
    private final Logger logger;

    public DefaultAnalysisService(ChessEngine engine,
                                  AccuracyCalculationService accuracy,
                                  MoveClassificationService classifier,
                                  GameRepository gameRepository,
                                  Logger logger
    ) {
        this.engine = engine;
        this.accuracy = accuracy;
        this.classifier = classifier;
        this.gameRepository = gameRepository;
        this.san = new SanNotationService();
        this.uci = new UciNotationService();
        this.logger = logger;
    }

    @Override
    public GameAnalysisDTO analyzeGame(Game game) {
        if (game.getAnalysis() != null) {
            logger.info("analysis.cached gameId=" + game.getId());
            return existingGameAnalysis(game);
        }
        logAnalysisStart(game);

        List<MoveEval> analyzed = getGameMovesEval(game);
        Map<MoveCategory, List<MoveAnalysisDTO>> buckets = initializeBuckets();
        AccuracyTotals totals = collectAccuracyTotals(analyzed, buckets);

        double accWhite = calculateSideAccuracy(totals.whitePenalty, totals.whiteWeight);
        double accBlack = calculateSideAccuracy(totals.blackPenalty, totals.blackWeight);

        var analysis = new GameAnalysis();
        analysis.setAccuracyWhite(accWhite);
        analysis.setAccuracyBlack(accBlack);
        analysis.setBlunders(buckets.get(MoveCategory.BLUNDER).size());
        analysis.setInaccuracies(buckets.get(MoveCategory.INACCURACY).size());
        analysis.setMistakes(buckets.get(MoveCategory.MISTAKE).size());
        analysis.setAnalyzedAt(OffsetDateTime.now());
        game.setAnalysis(analysis);
        gameRepository.save(game);

        logger.info("analysis.complete gameId=" + game.getId()
                + " accuracyWhite=" + accWhite
                + " accuracyBlack=" + accBlack
                + " blunders=" + buckets.get(MoveCategory.BLUNDER).size());
        return new GameAnalysisDTO(
                game.getId(),
                game.getUserId(),
                accWhite,
                accBlack,
                buckets.get(MoveCategory.BEST_MOVE),
                buckets.get(MoveCategory.GOOD_MOVE),
                buckets.get(MoveCategory.INACCURACY),
                buckets.get(MoveCategory.MISTAKE),
                buckets.get(MoveCategory.BLUNDER)
        );
    }

    private void logAnalysisStart(Game game) {
        logger.info("analysis.start gameId=" + game.getId()
                + " userId=" + game.getUserId()
                + " moves=" + game.getMoves().size());
    }

    private Map<MoveCategory, List<MoveAnalysisDTO>> initializeBuckets() {
        Map<MoveCategory, List<MoveAnalysisDTO>> buckets = new EnumMap<>(MoveCategory.class);
        for (MoveCategory category : MoveCategory.values()) {
            buckets.put(category, new ArrayList<>());
        }
        return buckets;
    }

    private AccuracyTotals collectAccuracyTotals(
            List<MoveEval> analyzedMoves,
            Map<MoveCategory, List<MoveAnalysisDTO>> buckets
    ) {
        AccuracyTotals totals = new AccuracyTotals();

        for (MoveEval entry : analyzedMoves) {
            MoveAnalysisDTO analysisDto = entry.analysis();
            MoveCategory category = classifier.classifyMove(analysisDto);
            buckets.get(category).add(analysisDto);

            Color side = sideForPly(entry.move().getPlyIndex());
            long moveNumber = moveNumberForPly(entry.move().getPlyIndex());
            double weight = accuracy.moveWeight(moveNumber);
            double penalty = accuracy.movePenalty(category, moveNumber);
            totals.add(side, penalty, weight);
        }

        return totals;
    }

    private static Color sideForPly(int plyIndex) {
        return (plyIndex % 2 == 1) ? Color.WHITE : Color.BLACK;
    }

    private static long moveNumberForPly(int plyIndex) {
        return (plyIndex + 1L) / 2L;
    }

    private double calculateSideAccuracy(double penalty, double weight) {
        if (weight > 0) {
            return accuracy.calculateAccuracy(penalty, weight);
        }
        return 100.0;
    }

    private List<MoveEval> getGameMovesEval(Game game) {
        List<MoveEval> out = new ArrayList<>();
        Set<GameMove> moves = game.getMoves();

        for (GameMove gm : moves) {
            Long moveNum = (long) Math.ceil(gm.getPlyIndex() / 2.0);
            String fenBefore = gm.getPositionFEN();
            String sanText = gm.getSan();

            Position pos = Position.fromFEN(fenBefore);
            Move mv = san.sanToMove(sanText, pos);
            String uciText = uci.moveToUci(mv);

            MoveDTO dto = new MoveDTO(moveNum, fenBefore, sanText, uciText);
            var analysisDto = analyzeMove(dto);
            Double cpLoss = null;
            if (analysisDto.mateScore() == null && analysisDto.bestMoveEval() != null)
                cpLoss = Math.abs(analysisDto.bestMoveEval() - analysisDto.playerMoveEval());
            var analysis = new GameMoveAnalysis(
                    analysisDto.playerMoveEval(),
                    analysisDto.mateScore(),
                    analysisDto.bestMove().moveUCI(),
                    cpLoss,
                    classifier.classifyMove(analysisDto)
            );
            gm.setAnalysis(analysis);

            out.add(new MoveEval(gm, analysisDto));
        }
        return out;
    }

    private record MoveEval(GameMove move, MoveAnalysisDTO analysis) {}

    private static final class AccuracyTotals {
        private double whitePenalty;
        private double whiteWeight;
        private double blackPenalty;
        private double blackWeight;

        private void add(Color side, double penalty, double weight) {
            if (side == Color.WHITE) {
                whitePenalty += penalty;
                whiteWeight += weight;
                return;
            }
            blackPenalty += penalty;
            blackWeight += weight;
        }
    }

    @Override
    public MoveAnalysisDTO analyzeMove(MoveDTO move) {
        long startedAtNanos = System.nanoTime();
        Position pos = Position.fromFEN(move.positionFEN());
        String uciText = resolveUci(move, pos);
        logMoveAnalysisStart(move, uciText);

        try {
            var request = buildEngineRequest(move, uciText);
            var response = engine.analyzeMove(request);
            Integer mateScore = resolveMateScore(response, pos, uciText);
            MoveAnalysisDTO result = toMoveAnalysisDto(move, response, mateScore);
            logMoveAnalysisComplete(move, uciText, response, mateScore, startedAtNanos, result);
            return result;
        } catch (RuntimeException ex) {
            long durationMs = (System.nanoTime() - startedAtNanos) / 1_000_000L;
            logger.error(
                    "move.analysis.failed moveNum=" + move.moveNum()
                            + " uci=" + uciText
                            + " durationMs=" + durationMs
                            + " reason=" + ex.getClass().getSimpleName()
                            + ": " + ex.getMessage()
            );
            throw ex;
        }
    }

    private String resolveUci(MoveDTO move, Position position) {
        String uciText = move.moveUCI();
        if (uciText != null && !uciText.isBlank()) {
            return uciText;
        }
        Move moveFromSan = san.sanToMove(move.moveSAN(), position);
        return uci.moveToUci(moveFromSan);
    }

    private void logMoveAnalysisStart(MoveDTO move, String uciText) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug(
                "move.analysis.start moveNum={} san={} uci={} fenLength={}",
                move.moveNum(),
                move.moveSAN(),
                uciText,
                move.positionFEN() == null ? 0 : move.positionFEN().length()
        );
    }

    private static EngineMoveRequest buildEngineRequest(MoveDTO move, String uciText) {
        return new EngineMoveRequest(
                move.positionFEN(),
                uciText,
                8,
                120,
                null,
                1
        );
    }

    private Integer resolveMateScore(
            ru.chessinsight.application.game.analysis.engine.dto.EngineMoveAnalysis response,
            Position position,
            String uciText
    ) {
        Integer mateScore = response.mateScore();
        if (mateScore != null) {
            return mateScore;
        }
        Move played = uci.uciToMove(uciText, position);
        Position after = MoveMaker.apply(position, played);
        var validator = new ru.chessinsight.domain.chess.move.service.MoveValidator();
        boolean inCheck = validator.isKingInCheck(after, after.sideToMove());
        boolean noMoves = validator.sideLegalMoves(after, after.sideToMove()).isEmpty();
        return inCheck && noMoves ? 0 : null;
    }

    private static MoveAnalysisDTO toMoveAnalysisDto(
            MoveDTO move,
            ru.chessinsight.application.game.analysis.engine.dto.EngineMoveAnalysis response,
            Integer mateScore
    ) {
        Double playedEval = (response.evalCp() != null)
                ? (response.evalCp() / 100.0)
                : (mateScore != null ? Double.POSITIVE_INFINITY : 0.0);
        Double bestEval = (response.evalCp() != null && response.cpLoss() != null)
                ? ((response.evalCp() + response.cpLoss()) / 100.0)
                : playedEval;
        var bestMoveDTO = new MoveDTO(
                move.moveNum(),
                response.positionFEN(),
                response.bestMoveSan(),
                response.bestMoveUci()
        );
        return new MoveAnalysisDTO(
                response.positionFEN(),
                bestMoveDTO,
                bestEval,
                playedEval,
                mateScore
        );
    }

    private void logMoveAnalysisComplete(
            MoveDTO move,
            String uciText,
            ru.chessinsight.application.game.analysis.engine.dto.EngineMoveAnalysis response,
            Integer mateScore,
            long startedAtNanos,
            MoveAnalysisDTO result
    ) {
        long durationMs = (System.nanoTime() - startedAtNanos) / 1_000_000L;
        logger.info(
                "move.analysis.complete moveNum=" + move.moveNum()
                        + " uci=" + uciText
                        + " durationMs=" + durationMs
                        + " eval=" + result.playerMoveEval()
                        + " mate=" + mateScore
        );
        if (log.isDebugEnabled()) {
            log.debug(
                    "move.analysis.result moveNum={} bestUci={} bestSan={} bestEval={} playerEval={} cpLoss={}",
                    move.moveNum(),
                    response.bestMoveUci(),
                    response.bestMoveSan(),
                    result.bestMoveEval(),
                    result.playerMoveEval(),
                    response.cpLoss()
            );
        }
    }

    private GameAnalysisDTO existingGameAnalysis(Game game) {
        var analysis = game.getAnalysis();
        Map<MoveCategory, List<MoveAnalysisDTO>> buckets = new EnumMap<>(MoveCategory.class);
        for (MoveCategory c : MoveCategory.values()) {
            buckets.put(c, new ArrayList<>());
        }
        for (var move : game.getMoves()) {
            var moveAnalysis = move.getAnalysis();
            var pos = Position.fromFEN(move.getPositionFEN());
            Move bestMove = uci.uciToMove(moveAnalysis.bestUCI(), pos);
            String bestSAN = san.moveToSan(bestMove, pos);
            Double bestMoveEval = null;
            if (moveAnalysis.mateScore() == null
                    && moveAnalysis.cpLoss() != null
                    && moveAnalysis.evalCp() != null) {
                double sign = (pos.sideToMove() == Color.WHITE) ? 1.0 : -1.0;
                bestMoveEval = moveAnalysis.evalCp() + (sign * moveAnalysis.cpLoss());
            }
            buckets.get(moveAnalysis.category()).add(
                    new MoveAnalysisDTO(
                            move.getPositionFEN(),
                            new MoveDTO(
                                    (long) move.getPlyIndex(),
                                    move.getPositionFEN(),
                                    moveAnalysis.bestUCI(),
                                    bestSAN
                            ),
                            bestMoveEval,
                            moveAnalysis.evalCp(),
                            moveAnalysis.mateScore()
                    )
            );
        }
        return new GameAnalysisDTO(
                game.getId(),
                game.getUserId(),
                analysis.getAccuracyWhite(),
                analysis.getAccuracyBlack(),
                buckets.get(MoveCategory.BEST_MOVE),
                buckets.get(MoveCategory.GOOD_MOVE),
                buckets.get(MoveCategory.INACCURACY),
                buckets.get(MoveCategory.MISTAKE),
                buckets.get(MoveCategory.BLUNDER)
        );
    }
}
