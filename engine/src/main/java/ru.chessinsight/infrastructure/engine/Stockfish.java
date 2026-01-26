package ru.chessinsight.infrastructure.engine;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import ru.chessinsight.application.game.analysis.engine.ChessEngine;
import ru.chessinsight.application.game.analysis.engine.dto.*;
import ru.chessinsight.application.game.analysis.engine.exception.EngineException;
import ru.chessinsight.domain.chess.move.model.Move;
import ru.chessinsight.domain.chess.move.notation.service.SanNotationService;
import ru.chessinsight.domain.chess.move.notation.service.UciNotationService;
import ru.chessinsight.domain.chess.move.service.MoveMaker;
import ru.chessinsight.domain.chess.position.model.Position;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
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

        isReady();
        sendCommand("ucinewgame");
        if (multipv > 1) {
            sendCommand("setoption name MultiPV value " + multipv);
        }
        sendCommand("position fen " + fen);
        if (request.movetimeMs() != null) {
            sendCommand("go movetime " + request.movetimeMs());
        } else {
            sendCommand("go depth " + depth);
        }

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
                    Matcher mb = BESTMOVE.matcher(line);
                    if (mb.find()) bestFirstUci = mb.group(1);
                    break;
                }
                if (!INFO_GENERIC.matcher(line).find()) continue;

                Integer d = findInt(INFO_DEPTH, line);
                if (d != null) finalDepth = d;
                Integer sd = findInt(INFO_SELDEPTH, line);
                if (sd != null) finalSelDepth = sd;

                Integer idx = findInt(INFO_MULTIPV, line);
                if (idx == null) idx = 1;

                Integer cp = findInt(INFO_SCORE_CP, line);
                Integer mate = findInt(INFO_SCORE_MATE, line);
                String pvText = findText(INFO_PV, line);

                if (pvText != null) {
                    List<String> pvUci = Arrays.asList(pvText.trim().split("\\s+"));
                    if (pvUci.size() > pvLimit) pvUci = pvUci.subList(0, pvLimit);
                    Integer finalDepth1 = finalDepth;
                    Integer finalSelDepth1 = finalSelDepth;
                    List<String> finalPvUci = pvUci;
                    aggByIdx.compute(idx, (k, v) -> {
                        if (v == null) v = new LineAgg();
                        v.pvUci = finalPvUci;
                        v.cp = cp;
                        v.mate = mate;
                        v.depth = finalDepth1;
                        v.selDepth = finalSelDepth1;
                        return v;
                    });
                    if (idx == 1) {
                        lastBestPvUci = pvUci;
                        bestCp = cp;
                        bestMate = mate;
                    }
                }
            }
        } catch (IOException e) {
            throw new EngineException("Error reading engine output: " + e.getMessage());
        }

        if (lastBestPvUci == null) {
            if (bestFirstUci != null) {
                lastBestPvUci = Collections.singletonList(bestFirstUci);
            } else {
                throw new EngineException("Engine did not provide a PV or bestmove");
            }
        }


        Position start = Position.fromFEN(fen);
        List<String> bestPvSan = toSanPv(start, lastBestPvUci);
        String bestMoveSan = bestFirstUci != null ? toSanPv(start, Collections.singletonList(bestFirstUci)).getFirst() : null;

        List<CandidateLine> candidates = aggByIdx.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    int idx = e.getKey();
                    LineAgg a = e.getValue();
                    List<String> sanLine = toSanPv(Position.fromFEN(fen), a.pvUci);
                    return new CandidateLine(idx, a.pvUci, sanLine, a.cp, a.mate);
                })
                .collect(Collectors.toList());

        return new EnginePositionAnalysis(
                fen,
                finalDepth, finalSelDepth,
                bestFirstUci, bestMoveSan,
                lastBestPvUci, bestPvSan,
                bestCp, bestMate,
                candidates
        );
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
