package ru.chessinsight.application.game.analysis.service.impl;

import org.springframework.stereotype.Service;
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
import ru.chessinsight.domain.chess.position.model.Position;
import ru.chessinsight.domain.chess.piece.model.Color;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.model.GameAnalysis;
import ru.chessinsight.domain.game.model.GameMove;
import ru.chessinsight.domain.game.model.GameMoveAnalysis;
import ru.chessinsight.domain.game.repository.GameRepository;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class DefaultAnalysisService implements AnalysisService {

    private final ChessEngine engine;
    private final AccuracyCalculationService accuracy;
    private final MoveClassificationService classifier;
    private final GameRepository gameRepository;
    private final SanNotationService san;
    private final UciNotationService uci;

    public DefaultAnalysisService(ChessEngine engine,
                                  AccuracyCalculationService accuracy,
                                  MoveClassificationService classifier,
                                  GameRepository gameRepository
    ) {
        this.engine = engine;
        this.accuracy = accuracy;
        this.classifier = classifier;
        this.gameRepository = gameRepository;
        this.san = new SanNotationService();
        this.uci = new UciNotationService();
    }

    @Override
    public GameAnalysisDTO analyzeGame(Game game) {
        if (game.getAnalysis() != null) {
            return existingGameAnalysis(game);
        }
        var analysis = new GameAnalysis();
        List<MoveAnalysisDTO> analyzed = getGameMovesEval(game);

        Map<MoveCategory, List<MoveAnalysisDTO>> buckets = new EnumMap<>(MoveCategory.class);
        for (MoveCategory c : MoveCategory.values()) {
            buckets.put(c, new ArrayList<>());
        }

        double whitePenalty = 0.0, whiteWeight = 0.0;
        double blackPenalty = 0.0, blackWeight = 0.0;

        for (MoveAnalysisDTO ma : analyzed) {
            MoveCategory cat = classifier.classifyMove(ma);
            buckets.get(cat).add(ma);

            Position pos = Position.fromFEN(ma.positionFEN());
            Color side = pos.sideToMove();

            long moveNum = ma.bestMove().moveNum() != null ? ma.bestMove().moveNum() : 0L;
            double w = accuracy.moveWeight(moveNum);
            double p = accuracy.movePenalty(cat, moveNum);

            if (side == Color.WHITE) {
                whitePenalty += p;
                whiteWeight  += w;
            } else {
                blackPenalty += p;
                blackWeight  += w;
            }
        }

        double accWhite = (whiteWeight > 0) ? accuracy.calculateAccuracy(whitePenalty, whiteWeight) : 100.0;
        double accBlack = (blackWeight > 0) ? accuracy.calculateAccuracy(blackPenalty, blackWeight) : 100.0;

        analysis.setAccuracyWhite(accWhite);
        analysis.setAccuracyBlack(accBlack);
        analysis.setBlunders(buckets.get(MoveCategory.BLUNDER).size());
        analysis.setInaccuracies(buckets.get(MoveCategory.INACCURACY).size());
        analysis.setMistakes(buckets.get(MoveCategory.MISTAKE).size());
        analysis.setAnalyzedAt(OffsetDateTime.now());
        game.setAnalysis(analysis);
        gameRepository.save(game);

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

    private List<MoveAnalysisDTO> getGameMovesEval(Game game) {
        List<MoveAnalysisDTO> out = new ArrayList<>();
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

            out.add(analysisDto);
        }
        return out;
    }

    @Override
    public MoveAnalysisDTO analyzeMove(MoveDTO move) {
        String uciText = move.moveUCI();
        if (uciText == null || uciText.isBlank()) {
            Position pos = Position.fromFEN(move.positionFEN());
            Move mv = san.sanToMove(move.moveSAN(), pos);
            uciText = uci.moveToUci(mv);
        }

        var req = new EngineMoveRequest(
                move.positionFEN(),
                uciText,
                8,
                120, null,
               1
        );

        var res = engine.analyzeMove(req);

        Double playedEval = (res.evalCp() != null) ? (res.evalCp() / 100.0) : (res.mateScore() != null ? Double.POSITIVE_INFINITY : 0.0);

        Double bestEval = (res.evalCp() != null && res.cpLoss() != null)
                ? ((res.evalCp() + res.cpLoss()) / 100.0)
                : playedEval;

        var bestMoveDTO = new MoveDTO(
                move.moveNum(),
                res.positionFEN(),
                res.bestMoveSan(),
                res.bestMoveUci()
        );

        return new MoveAnalysisDTO(
                res.positionFEN(),
                bestMoveDTO,
                bestEval,
                playedEval,
                res.mateScore()
        );
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
            if (moveAnalysis.mateScore() == null) {
                bestMoveEval = moveAnalysis.cpLoss() * (pos.sideToMove() == Color.WHITE ? -1 : 11)
                        + moveAnalysis.evalCp();
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
