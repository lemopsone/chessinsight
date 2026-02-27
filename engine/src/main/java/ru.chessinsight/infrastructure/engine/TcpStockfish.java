package ru.chessinsight.infrastructure.engine;

import org.springframework.beans.factory.annotation.Value;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

    private static final int CONNECT_MAX_ATTEMPTS = 8;
    private static final int CONNECT_TIMEOUT_MS = 1500;
    private static final int CONNECT_BACKOFF_MS = 300;
    private static final int DEFAULT_READ_TIMEOUT_MS = 10_000;
    private static final int DEFAULT_POOL_SIZE = 8;

    private final UciNotationService uci = new UciNotationService();
    private final SanNotationService san = new SanNotationService();

    private final String host;
    private final int port;
    private final int defaultDepth;
    private final int threads;
    private final int hashMb;
    private final boolean ponder;
    private final int readTimeoutMs;
    private final int poolSize;

    private final BlockingQueue<EngineSession> availableSessions;
    private final Set<EngineSession> allSessions;
    private final AtomicInteger createdSessions;

    private volatile String engineName = "Unknown";
    private volatile String engineAuthor = null;
    private volatile boolean closed = false;

    public TcpStockfish(
            @Value("${engine.stockfish.host:stockfish}") String host,
            @Value("${engine.stockfish.port:5000}") int port,
            @Value("${engine.stockfish.defaultDepth:15}") int defaultDepth,
            @Value("${engine.stockfish.threads:2}") int threads,
            @Value("${engine.stockfish.hash:128}") int hashMb,
            @Value("${engine.stockfish.ponder:false}") boolean ponder,
            @Value("${engine.stockfish.readTimeoutMs:" + DEFAULT_READ_TIMEOUT_MS + "}") int readTimeoutMs,
            @Value("${engine.stockfish.poolSize:" + DEFAULT_POOL_SIZE + "}") int poolSize,
            @Value("${engine.stockfish.eagerWarmup:false}") boolean eagerWarmup
    ) {
        this.host = host;
        this.port = port;
        this.defaultDepth = defaultDepth;
        this.threads = Math.max(1, threads);
        this.hashMb = Math.max(16, hashMb);
        this.ponder = ponder;
        this.readTimeoutMs = Math.max(1_000, readTimeoutMs);
        this.poolSize = Math.max(1, poolSize);
        this.availableSessions = new ArrayBlockingQueue<>(this.poolSize, true);
        this.allSessions = ConcurrentHashMap.newKeySet();
        this.createdSessions = new AtomicInteger(0);
        if (eagerWarmup) {
            warmUpPool();
        }
    }

    @Override
    public EngineInfo info() {
        return new EngineInfo(engineName, engineAuthor, null, Collections.emptyMap());
    }

    @Override
    public EnginePositionAnalysis analyzePosition(EngineAnalysisRequest request) throws EngineException {
        EngineSession session = borrowSession();
        boolean discard = false;
        try {
            return analyzePositionWithSession(session, request);
        } catch (EngineException e) {
            discard = true;
            throw e;
        } finally {
            if (discard) {
                discardSession(session);
            } else {
                releaseSession(session);
            }
        }
    }

    @Override
    public EngineMoveAnalysis analyzeMove(EngineMoveRequest request) throws EngineException {
        EngineSession session = borrowSession();
        boolean discard = false;
        try {
            final String fen = request.positionFEN();
            final String playedUci = request.playedMoveUci();
            final int depth = request.effectiveDepth(defaultDepth);
            final int pvLimit = request.effectivePvLimit(80);

            EnginePositionAnalysis pos = analyzePositionWithSession(
                    session,
                    new EngineAnalysisRequest(fen, depth, request.movetimeMs(), request.nodes(), request.pvLimit(), pvLimit)
            );

            Position posBefore = Position.fromFEN(fen);
            Move played = uci.uciToMove(playedUci, posBefore);
            Position after = MoveMaker.apply(posBefore, played);
            EnginePositionAnalysis afterAnalysis = analyzePositionWithSession(
                    session,
                    new EngineAnalysisRequest(after.toFEN(), depth, request.movetimeMs(), request.nodes(), request.pvLimit(), pvLimit)
            );

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
                    playedUci,
                    playedSan,
                    evalCp,
                    mateScore,
                    pos.bestMoveUci(),
                    pos.bestMoveSan(),
                    cpLoss,
                    pos.pvUci(),
                    pos.pvSan()
            );
        } catch (EngineException e) {
            discard = true;
            throw e;
        } finally {
            if (discard) {
                discardSession(session);
            } else {
                releaseSession(session);
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        EngineSession pooled;
        while ((pooled = availableSessions.poll()) != null) {
            closeSession(pooled);
        }
        for (EngineSession session : new ArrayList<>(allSessions)) {
            closeSession(session);
        }
        allSessions.clear();
        createdSessions.set(0);
    }

    private EnginePositionAnalysis analyzePositionWithSession(EngineSession session, EngineAnalysisRequest request) throws EngineException {
        final String fen = request.positionFEN();
        final int depth = request.effectiveDepth(defaultDepth);
        final int multipv = request.effectiveMultiPv();
        final int pvLimit = request.effectivePvLimit(80);

        isReady(session);
        sendCommand(session, "ucinewgame");
        if (multipv > 1) {
            sendCommand(session, "setoption name MultiPV value " + multipv);
        }
        sendCommand(session, "position fen " + fen);
        if (request.movetimeMs() != null) {
            sendCommand(session, "go movetime " + request.movetimeMs());
        } else {
            sendCommand(session, "go depth " + depth);
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
            while ((line = session.reader.readLine()) != null) {
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
                if (!INFO_GENERIC.matcher(line).find()) {
                    continue;
                }

                Integer d = findInt(INFO_DEPTH, line);
                if (d != null) {
                    finalDepth = d;
                }
                Integer sd = findInt(INFO_SELDEPTH, line);
                if (sd != null) {
                    finalSelDepth = sd;
                }

                Integer idx = findInt(INFO_MULTIPV, line);
                if (idx == null) {
                    idx = 1;
                }

                Integer cp = findInt(INFO_SCORE_CP, line);
                Integer mate = findInt(INFO_SCORE_MATE, line);
                String pvText = findText(INFO_PV, line);

                if (pvText != null) {
                    List<String> pvUci = Arrays.asList(pvText.trim().split("\\s+"));
                    if (pvUci.size() > pvLimit) {
                        pvUci = pvUci.subList(0, pvLimit);
                    }
                    Integer finalDepth1 = finalDepth;
                    Integer finalSelDepth1 = finalSelDepth;
                    List<String> finalPvUci = pvUci;
                    aggByIdx.compute(idx, (k, v) -> {
                        if (v == null) {
                            v = new LineAgg();
                        }
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
                        finalDepth,
                        finalSelDepth,
                        null,
                        null,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        bestCp,
                        bestMate,
                        Collections.emptyList()
                );
            }
        }

        Position start = Position.fromFEN(fen);
        List<String> bestPvSan = toSanPv(start, lastBestPvUci);
        String bestMoveSan = bestFirstUci != null
                ? toSanPv(start, Collections.singletonList(bestFirstUci)).getFirst()
                : null;

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
                finalDepth,
                finalSelDepth,
                bestFirstUci,
                bestMoveSan,
                lastBestPvUci,
                bestPvSan,
                bestCp,
                bestMate,
                candidates
        );
    }

    private EngineSession borrowSession() throws EngineException {
        if (closed) {
            throw new EngineException("Stockfish engine is closed");
        }

        EngineSession fromPool = availableSessions.poll();
        if (fromPool != null) {
            return fromPool;
        }

        while (true) {
            if (closed) {
                throw new EngineException("Stockfish engine is closed");
            }

            int current = createdSessions.get();
            if (current < poolSize && createdSessions.compareAndSet(current, current + 1)) {
                try {
                    EngineSession created = createConnectedSession();
                    allSessions.add(created);
                    return created;
                } catch (EngineException e) {
                    createdSessions.decrementAndGet();
                    throw e;
                }
            }

            try {
                EngineSession waited = availableSessions.poll(readTimeoutMs, TimeUnit.MILLISECONDS);
                if (waited != null) {
                    return waited;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new EngineException("Interrupted while waiting for Stockfish session");
            }

            throw new EngineException("Timeout waiting for free Stockfish session (poolSize=" + poolSize + ")");
        }
    }

    private void releaseSession(EngineSession session) {
        if (session == null) {
            return;
        }
        if (closed || !session.isAlive()) {
            discardSession(session);
            return;
        }
        if (!availableSessions.offer(session)) {
            discardSession(session);
        }
    }

    private void discardSession(EngineSession session) {
        if (session == null) {
            return;
        }
        closeSession(session);
        if (allSessions.remove(session)) {
            createdSessions.decrementAndGet();
        }
    }

    private EngineSession createConnectedSession() throws EngineException {
        EngineException lastError = null;
        for (int attempt = 1; attempt <= CONNECT_MAX_ATTEMPTS; attempt++) {
            Socket socket = null;
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
                socket.setSoTimeout(readTimeoutMs);
                EngineSession session = new EngineSession(
                        socket,
                        new BufferedReader(new InputStreamReader(socket.getInputStream())),
                        new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
                );
                initialize(session);
                return session;
            } catch (IOException | EngineException e) {
                lastError = (e instanceof EngineException)
                        ? (EngineException) e
                        : new EngineException("Failed to connect to Stockfish at " + host + ":" + port + ": " + e.getMessage());
                closeSocketQuietly(socket);
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

    private void warmUpPool() {
        List<EngineSession> warmed = new ArrayList<>(poolSize);
        try {
            for (int i = 0; i < poolSize; i++) {
                warmed.add(createConnectedSession());
            }
            for (EngineSession session : warmed) {
                allSessions.add(session);
                createdSessions.incrementAndGet();
                if (!availableSessions.offer(session)) {
                    throw new EngineException("Failed to add warmed Stockfish session to pool");
                }
            }
        } catch (EngineException e) {
            for (EngineSession session : warmed) {
                closeSession(session);
            }
            allSessions.clear();
            availableSessions.clear();
            createdSessions.set(0);
            throw new EngineException("Failed to warm up Stockfish pool: " + e.getMessage());
        }
    }

    private void initialize(EngineSession session) throws EngineException {
        sendCommand(session, "uci");
        String sessionName = null;
        String sessionAuthor = null;
        try {
            String line;
            while ((line = session.reader.readLine()) != null) {
                if ("uciok".equals(line)) {
                    break;
                }
                Matcher mn = ID_NAME.matcher(line);
                if (mn.find()) {
                    sessionName = mn.group(1).trim();
                }
                Matcher ma = ID_AUTHOR.matcher(line);
                if (ma.find()) {
                    sessionAuthor = ma.group(1).trim();
                }
            }
        } catch (IOException e) {
            throw new EngineException("Engine did not respond to 'uci': " + e.getMessage());
        }

        if (sessionName != null && !sessionName.isBlank()) {
            engineName = sessionName;
        }
        if (sessionAuthor != null && !sessionAuthor.isBlank()) {
            engineAuthor = sessionAuthor;
        }

        sendCommand(session, "setoption name Threads value " + threads);
        sendCommand(session, "setoption name Hash value " + hashMb);
        sendCommand(session, "setoption name Ponder value " + ponder);
        isReady(session);
    }

    private void isReady(EngineSession session) throws EngineException {
        sendCommand(session, "isready");
        try {
            readUntil(session, Duration.ofSeconds(5), "readyok");
        } catch (IOException e) {
            throw new EngineException("Engine did not respond to 'isready': " + e.getMessage());
        }
    }

    private void sendCommand(EngineSession session, String cmd) throws EngineException {
        if (session == null || session.writer == null) {
            throw new EngineException("Stockfish connection is not initialized");
        }
        try {
            session.writer.write(cmd + "\n");
            session.writer.flush();
        } catch (IOException e) {
            throw new EngineException("Failed to send command: " + cmd);
        }
    }

    private void readUntil(EngineSession session, Duration timeout, String expected) throws IOException, EngineException {
        long end = System.currentTimeMillis() + timeout.toMillis();
        String line;
        while (System.currentTimeMillis() < end && (line = session.reader.readLine()) != null) {
            if (line.contains(expected)) {
                return;
            }
        }
        throw new EngineException("Timeout waiting for " + expected);
    }

    private void closeSession(EngineSession session) {
        if (session == null) {
            return;
        }
        try {
            sendCommand(session, "quit");
        } catch (Exception ignored) {
        }
        closeSocketQuietly(session.socket);
    }

    private void closeSocketQuietly(Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (Exception ignored) {
        }
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

    private static final class LineAgg {
        Integer depth;
        Integer selDepth;
        List<String> pvUci = Collections.emptyList();
        Integer cp;
        Integer mate;
    }

    private static final class EngineSession {
        final Socket socket;
        final BufferedReader reader;
        final BufferedWriter writer;

        EngineSession(Socket socket, BufferedReader reader, BufferedWriter writer) {
            this.socket = socket;
            this.reader = reader;
            this.writer = writer;
        }

        boolean isAlive() {
            return socket != null && socket.isConnected() && !socket.isClosed();
        }
    }
}
