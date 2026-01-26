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

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TcpStockfish implements ChessEngine, AutoCloseable {

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

    private final UciNotationService uci = new UciNotationService();
    private final SanNotationService san = new SanNotationService();

    private static final int CONNECT_MAX_ATTEMPTS = 8;
    private static final int CONNECT_TIMEOUT_MS = 1500;
    private static final int CONNECT_BACKOFF_MS = 300;

    private final String host;
    private final int port;
    private final int defaultDepth;
    private final int threads;
    private final int hashMb;
    private final boolean ponder;
    private final Object connectionLock = new Object();

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    private String engineName = "Unknown";
    private String engineAuthor = null;

    public TcpStockfish(
            @Value("${engine.stockfish.host:stockfish}") String host,
            @Value("${engine.stockfish.port:5000}") int port,
            @Value("${engine.stockfish.defaultDepth:15}") int defaultDepth,
            @Value("${engine.stockfish.threads:2}") int threads,
            @Value("${engine.stockfish.hash:128}") int hashMb,
            @Value("${engine.stockfish.ponder:false}") boolean ponder
    ) throws IOException, EngineException {
        this.host = host;
        this.port = port;
        this.defaultDepth = defaultDepth;
        this.threads = Math.max(1, threads);
        this.hashMb = Math.max(16, hashMb);
        this.ponder = ponder;
    }

    private void ensureConnected() throws EngineException {
        if (socket != null && socket.isConnected() && !socket.isClosed()) {
            return;
        }
        synchronized (connectionLock) {
            if (socket != null && socket.isConnected() && !socket.isClosed()) {
                return;
            }
            EngineException lastError = null;
            for (int attempt = 1; attempt <= CONNECT_MAX_ATTEMPTS; attempt++) {
                try {
                    Socket newSocket = new Socket();
                    newSocket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
                    this.socket = newSocket;
                    this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    initialize();
                    return;
                } catch (IOException | EngineException e) {
                    lastError = (e instanceof EngineException)
                            ? (EngineException) e
                            : new EngineException("Failed to connect to Stockfish at " + host + ":" + port + ": " + e.getMessage());
                    closeQuietly();
                    try {
                        Thread.sleep(CONNECT_BACKOFF_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw lastError;
                    }
                }
            }
            throw lastError != null ? lastError : new EngineException("Failed to connect to Stockfish");
        }
    }

    private void sendCommand(String cmd) throws EngineException {
        if (writer == null) {
            throw new EngineException("Stockfish connection is not initialized");
        }
        try {
            writer.write(cmd + "\n");
            writer.flush();
        } catch (IOException e) {
            throw new EngineException("Failed to send command: " + cmd);
        }
    }

    private void readUntil(Duration timeout, String expected) throws IOException, EngineException {
        long end = System.currentTimeMillis() + timeout.toMillis();
        String line;
        while (System.currentTimeMillis() < end && (line = reader.readLine()) != null) {
            if (line.contains(expected)) {
                return;
            }
        }
        throw new EngineException("Timeout waiting for " + expected);
    }

    private void initialize() throws EngineException {
        sendCommand("uci");
        try {
            String line;
            while ((line = reader.readLine()) != null) {
//                System.out.println("[Stockfish INIT] " + line);
                if ("uciok".equals(line)) break;
                Matcher mn = ID_NAME.matcher(line);
                if (mn.find()) engineName = mn.group(1).trim();
                Matcher ma = ID_AUTHOR.matcher(line);
                if (ma.find()) engineAuthor = ma.group(1).trim();
            }
        } catch (IOException e) {
            throw new EngineException("Engine did not respond to 'uci': " + e.getMessage());
        }

        sendCommand("setoption name Threads value " + threads);
        sendCommand("setoption name Hash value " + hashMb);
        sendCommand("setoption name Ponder value " + ponder);
//        System.out.println("[Stockfish INIT] " + "ponder");
        isReady();
//        System.out.println("isReady");
    }

    private void isReady() throws EngineException {
        sendCommand("isready");
        try {
            readUntil(Duration.ofSeconds(5), "readyok");
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
        ensureConnected();
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
//                System.out.println("[ANALYSIS] " + line);
                if (line.startsWith("bestmove")) {
                    Matcher mb = BESTMOVE.matcher(line);
                    if (mb.find()) {
                        bestFirstUci = mb.group(1);
                        if ("(none)".equals(bestFirstUci)) {
                            bestFirstUci = null;
                        }
                    }
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
                return new EnginePositionAnalysis(
                        fen,
                        finalDepth, finalSelDepth,
                        null, null,
                        Collections.emptyList(), Collections.emptyList(),
                        bestCp, bestMate,
                        Collections.emptyList()
                );
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
        ensureConnected();
        final String fen = request.positionFEN();
        final String playedUci = request.playedMoveUci();
        final int depth = request.effectiveDepth(defaultDepth);
        final int pvLimit = request.effectivePvLimit(80);

        EnginePositionAnalysis pos = analyzePosition(new EngineAnalysisRequest(fen, depth, request.movetimeMs(), request.nodes(), request.pvLimit(), pvLimit));

        Position posBefore = Position.fromFEN(fen);
        Move played = uci.uciToMove(playedUci, posBefore);
        Position after = MoveMaker.apply(posBefore, played);
        EnginePositionAnalysis afterAnalysis = analyzePosition(new EngineAnalysisRequest(after.toFEN(), depth, request.movetimeMs(), request.nodes(), request.pvLimit(), pvLimit));

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
    public void close() throws IOException {
        closeQuietly();
    }

    private void closeQuietly() {
        try {
            if (writer != null) {
                sendCommand("quit");
            }
        } catch (Exception ignored) {
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception ignored) {
        } finally {
            socket = null;
            reader = null;
            writer = null;
        }
    }
}
