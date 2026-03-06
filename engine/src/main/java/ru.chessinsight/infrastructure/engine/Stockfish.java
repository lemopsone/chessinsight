package ru.chessinsight.infrastructure.engine;

import org.springframework.beans.factory.annotation.Value;
import ru.chessinsight.application.game.analysis.engine.ChessEngine;
import ru.chessinsight.application.game.analysis.engine.dto.CandidateLine;
import ru.chessinsight.application.game.analysis.engine.dto.EngineAnalysisRequest;
import ru.chessinsight.application.game.analysis.engine.dto.EngineInfo;
import ru.chessinsight.application.game.analysis.engine.dto.EngineMoveAnalysis;
import ru.chessinsight.application.game.analysis.engine.dto.EngineMoveRequest;
import ru.chessinsight.application.game.analysis.engine.dto.EnginePositionAnalysis;
import ru.chessinsight.application.game.analysis.engine.exception.EngineException;
import ru.chessinsight.domain.chess.move.model.Move;
import ru.chessinsight.domain.chess.move.notation.service.SanNotationService;
import ru.chessinsight.domain.chess.move.notation.service.UciNotationService;
import ru.chessinsight.domain.chess.move.service.MoveMaker;
import ru.chessinsight.domain.chess.position.model.Position;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Stockfish extends ExecutableWrapper implements ChessEngine {

    private static final Pattern INFO_GENERIC = Pattern.compile("\\binfo\\b.*");
    private static final Pattern INFO_DEPTH = Pattern.compile("\\bdepth\\s+(\\d+)");
    private static final Pattern INFO_SELDEPTH = Pattern.compile("\\bseldepth\\s+(\\d+)");
    private static final Pattern INFO_MULTIPV = Pattern.compile("\\bmultipv\\s+(\\d+)");
    private static final Pattern INFO_SCORE_CP = Pattern.compile("\\bscore\\s+cp\\s+(-?\\d+)");
    private static final Pattern INFO_SCORE_MATE = Pattern.compile("\\bscore\\s+mate\\s+(-?\\d+)");
    private static final Pattern INFO_PV = Pattern.compile("\\bpv\\s+(.+)$");
    private static final Pattern ID_NAME = Pattern.compile("^id\\s+name\\s+(.+)$");
    private static final Pattern ID_AUTHOR = Pattern.compile("^id\\s+author\\s+(.+)$");
    private static final Pattern BESTMOVE = Pattern.compile("^bestmove\\s+(\\S+)");

    private final UciNotationService uci;
    private final SanNotationService san;

    private final int defaultDepth;
    private String engineName = "Unknown";
    private String engineAuthor = null;
    public Stockfish(@Value("${engine.stockfish.path}") String path,
                     @Value("${engine.stockfish.defaultDepth:15}") int defaultDepth) throws IOException, EngineException {
        super(path);
        this.defaultDepth = defaultDepth;
        this.uci = new UciNotationService();
        this.san = new SanNotationService();
        initialize();
    }

    private void initialize() throws EngineException {
        sendCommand("uci");
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("uciok")) break;
                Matcher mn = ID_NAME.matcher(line);
                if (mn.find()) engineName = mn.group(1).trim();
                Matcher ma = ID_AUTHOR.matcher(line);
                if (ma.find()) engineAuthor = ma.group(1).trim();
            }
        } catch (IOException e) {
            throw new EngineException("Engine did not respond to 'uci': " + e.getMessage());
        }
        sendCommand("setoption name Threads value 6");
        sendCommand("setoption name Hash value 512");
        sendCommand("setoption name Ponder value false");
        isReady();
    }

    private void isReady() throws EngineException {
        sendCommand("isready");
        try {
            readUntil(Duration.ofSeconds(10), "readyok");
        } catch (IOException e) {
            throw new EngineException("Engine did not respond to 'isready': " + e.getMessage());
        }
    }

    @Override
    public EngineInfo info() {
        return new EngineInfo(engineName, engineAuthor, null, Collections.emptyMap());
    }

    @Override
    public EnginePositionAnalysis analyzePosition(EngineAnalysisRequest request) throws EngineException {
        final String fen = request.positionFEN();
        final int depth = request.effectiveDepth(defaultDepth);
        final int multipv = request.effectiveMultiPv();
        final int pvLimit = request.effectivePvLimit(80);

        startPositionAnalysis(request, fen, depth, multipv);
        ParsedAnalysis parsed = readAnalysisOutput(pvLimit);
        List<String> bestPvUci = resolveBestPv(parsed);
        Position start = Position.fromFEN(fen);
        List<String> bestPvSan = toSanPv(start, bestPvUci);
        String bestMoveSan = parsed.bestFirstUci() != null
                ? toSanPv(start, Collections.singletonList(parsed.bestFirstUci())).getFirst()
                : null;
        List<CandidateLine> candidates = toCandidateLines(fen, parsed.aggByIdx());

        return new EnginePositionAnalysis(
                fen,
                parsed.finalDepth(),
                parsed.finalSelDepth(),
                parsed.bestFirstUci(),
                bestMoveSan,
                bestPvUci,
                bestPvSan,
                parsed.bestCp(),
                parsed.bestMate(),
                candidates
        );
    }

    private void startPositionAnalysis(EngineAnalysisRequest request, String fen, int depth, int multipv) throws EngineException {
        isReady();
        sendCommand("ucinewgame");
        if (multipv > 1) {
            sendCommand("setoption name MultiPV value " + multipv);
        }
        sendCommand("position fen " + fen);
        if (request.movetimeMs() != null) {
            sendCommand("go movetime " + request.movetimeMs());
            return;
        }
        sendCommand("go depth " + depth);
    }

    private ParsedAnalysis readAnalysisOutput(int pvLimit) throws EngineException {
        Map<Integer, LineAgg> aggByIdx = new HashMap<>();
        String bestFirstUci = null;
        Integer finalDepth = null;
        Integer finalSelDepth = null;
        Integer bestCp = null;
        Integer bestMate = null;
        List<String> lastBestPvUci = null;

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("bestmove")) {
                    bestFirstUci = extractBestMove(line);
                    break;
                }
                if (!INFO_GENERIC.matcher(line).find()) {
                    continue;
                }
                finalDepth = pickDepth(line, finalDepth);
                finalSelDepth = pickSelDepth(line, finalSelDepth);
                ParsedInfo parsedInfo = parseInfoLine(line, finalDepth, finalSelDepth, pvLimit);
                if (parsedInfo == null) {
                    continue;
                }
                mergeInfo(aggByIdx, parsedInfo);
                if (parsedInfo.idx() == 1) {
                    lastBestPvUci = parsedInfo.pvUci();
                    bestCp = parsedInfo.cp();
                    bestMate = parsedInfo.mate();
                }
            }
        } catch (IOException e) {
            throw new EngineException("Error reading engine output: " + e.getMessage());
        }

        return new ParsedAnalysis(aggByIdx, bestFirstUci, finalDepth, finalSelDepth, bestCp, bestMate, lastBestPvUci);
    }

    private static String extractBestMove(String line) {
        Matcher matcher = BESTMOVE.matcher(line);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static Integer pickDepth(String line, Integer currentDepth) {
        Integer value = findInt(INFO_DEPTH, line);
        return value != null ? value : currentDepth;
    }

    private static Integer pickSelDepth(String line, Integer currentSelDepth) {
        Integer value = findInt(INFO_SELDEPTH, line);
        return value != null ? value : currentSelDepth;
    }

    private static ParsedInfo parseInfoLine(String line, Integer finalDepth, Integer finalSelDepth, int pvLimit) {
        String pvText = findText(INFO_PV, line);
        if (pvText == null) {
            return null;
        }
        int idx = Optional.ofNullable(findInt(INFO_MULTIPV, line)).orElse(1);
        Integer cp = findInt(INFO_SCORE_CP, line);
        Integer mate = findInt(INFO_SCORE_MATE, line);
        List<String> pvUci = limitedPv(pvText, pvLimit);
        return new ParsedInfo(idx, pvUci, cp, mate, finalDepth, finalSelDepth);
    }

    private static List<String> limitedPv(String pvText, int pvLimit) {
        List<String> pvUci = Arrays.asList(pvText.trim().split("\\s+"));
        if (pvUci.size() <= pvLimit) {
            return pvUci;
        }
        return pvUci.subList(0, pvLimit);
    }

    private static void mergeInfo(Map<Integer, LineAgg> aggByIdx, ParsedInfo info) {
        aggByIdx.compute(info.idx(), (key, value) -> {
            LineAgg agg = value == null ? new LineAgg() : value;
            agg.pvUci = info.pvUci();
            agg.cp = info.cp();
            agg.mate = info.mate();
            agg.depth = info.depth();
            agg.selDepth = info.selDepth();
            return agg;
        });
    }

    private static List<String> resolveBestPv(ParsedAnalysis parsed) throws EngineException {
        if (parsed.lastBestPvUci() != null) {
            return parsed.lastBestPvUci();
        }
        if (parsed.bestFirstUci() != null) {
            return Collections.singletonList(parsed.bestFirstUci());
        }
        throw new EngineException("Engine did not provide a PV or bestmove");
    }

    private List<CandidateLine> toCandidateLines(String fen, Map<Integer, LineAgg> aggByIdx) {
        return aggByIdx.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> toCandidateLine(fen, entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private CandidateLine toCandidateLine(String fen, int idx, LineAgg agg) {
        List<String> sanLine = toSanPv(Position.fromFEN(fen), agg.pvUci);
        return new CandidateLine(idx, agg.pvUci, sanLine, agg.cp, agg.mate);
    }

    @Override
    public EngineMoveAnalysis analyzeMove(EngineMoveRequest request) throws EngineException {
        final String fen = request.positionFEN();
        final String playedUci = request.playedMoveUci();
        final int depth = request.effectiveDepth(defaultDepth);
        final int pvLimit = request.effectivePvLimit(80);

        EnginePositionAnalysis pos = analyzePosition(new EngineAnalysisRequest(fen, depth, null, null, 1, pvLimit));

        Position posBefore = Position.fromFEN(fen);
        Move played = uci.uciToMove(playedUci, posBefore);
        Position after = MoveMaker.apply(posBefore, played);
        EnginePositionAnalysis afterAnalysis = analyzePosition(new EngineAnalysisRequest(after.toFEN(), depth, null, null, 1, pvLimit));

        Integer evalCp = afterAnalysis.scoreCp();
        Integer mateScore = afterAnalysis.scoreMate();
        if (evalCp != null) {
            evalCp = -evalCp;
        }
        if (mateScore != null) {
            mateScore = -mateScore;
        }
        Integer cpLoss = (pos.scoreCp() != null && evalCp != null) ? (pos.scoreCp() - evalCp) : null;

        String playedSan = toSanPv(Position.fromFEN(fen), Collections.singletonList(playedUci)).getFirst();

        return new EngineMoveAnalysis(
                fen,
                playedUci, playedSan,
                evalCp, mateScore,
                pos.bestMoveUci(), pos.bestMoveSan(),
                cpLoss,
                pos.pvUci(), pos.pvSan()
        );
    }

    private static Integer findInt(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }
    private static String findText(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? m.group(1) : null;
    }

    private List<String> toSanPv(Position start, List<String> pvUci) {
        Position pos = start;
        List<String> out = new ArrayList<>(pvUci.size());
        for (String u : pvUci) {
            Move mv = uci.uciToMove(u, pos);
            out.add(san.moveToSan(mv, pos));
            pos = MoveMaker.apply(pos, mv);
        }
        return out;
    }

    private static class LineAgg {
        Integer depth;
        Integer selDepth;
        List<String> pvUci = Collections.emptyList();
        Integer cp;
        Integer mate;
    }

    private record ParsedInfo(int idx, List<String> pvUci, Integer cp, Integer mate, Integer depth, Integer selDepth) {}

    private record ParsedAnalysis(
            Map<Integer, LineAgg> aggByIdx,
            String bestFirstUci,
            Integer finalDepth,
            Integer finalSelDepth,
            Integer bestCp,
            Integer bestMate,
            List<String> lastBestPvUci
    ) {}

    @Override
    public String getOutput() throws IOException {
        StringBuilder builder = new StringBuilder();
        String text;
        while ((text = reader.readLine()) != null) {
            builder.append(text).append('\n');
        }
        return builder.toString();
    }
    @Override
    public void close() throws IOException {
        try {
            if (writer != null) {
                writer.write("quit\n");
                writer.flush();
            }
        } catch (Exception ignored) {}
        try {
            if (process != null) process.destroy();
        } finally {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
        }
    }

}
