package ru.chessinsight.application.game.service.impl;

import org.springframework.stereotype.Service;
import ru.chessinsight.application.common.logger.service.Logger;
import ru.chessinsight.application.exception.ApplicationException;
import ru.chessinsight.application.game.service.GameImportService;
import ru.chessinsight.domain.chess.move.notation.service.SanNotationService;
import ru.chessinsight.domain.chess.move.notation.service.UciNotationService;
import ru.chessinsight.domain.chess.move.service.MoveMaker;
import ru.chessinsight.domain.chess.position.model.Position;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.model.GameMove;
import ru.chessinsight.domain.game.model.GameResult;
import ru.chessinsight.domain.game.notation.service.pgn.PgnService;
import ru.chessinsight.domain.game.repository.GameRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ConcreteGameImportService implements GameImportService {
    private static final Pattern BRACE_COMMENT = Pattern.compile("\\{[^}]*\\}");
    private static final Pattern SEMICOLON_COMMENT = Pattern.compile(";.*");
    private static final Pattern VARIATION_PARENS = Pattern.compile("\\([^()]*\\)");
    private static final Pattern NAG = Pattern.compile("\\$\\d+");
    private static final Pattern MOVE_NUM = Pattern.compile("\\d+\\.{1,3}");
    private static final Pattern RESULT_TOKEN = Pattern.compile("(?i)(1-0|0-1|1/2-1/2|\\*)");

    private final PgnService pgnService;
    private final UciNotationService uciNotationService;
    private final SanNotationService sanNotationService;
    private final GameRepository gameRepository;
    private final Logger logger;

    public ConcreteGameImportService(GameRepository gameRepository, Logger logger) {
        this.gameRepository = gameRepository;
        this.logger = logger;
        this.pgnService = new PgnService();
        this.uciNotationService = new UciNotationService();
        this.sanNotationService = new SanNotationService();
    }

    @Override
    public UUID importFromPgn(UUID ownerId, String pgn, String startFen, String resultTag) {
        var game = pgnService.createEmptyGameFromPGN(pgn);
        GameResult parsedResult = parseResult(resultTag);
        if (parsedResult != null) {
            game.setResult(parsedResult);
        }
        var moves = pgnService.extractMovesFromGameMainline(game);
        var movesSet = new TreeSet<>(Comparator.comparing(GameMove::getPlyIndex));
        movesSet.addAll(moves);
        game.setMoves(movesSet);
        game.setUserId(ownerId);
        game = gameRepository.save(game);

        if (game != null) {
            logger.info("game.import.pgn success userId=" + ownerId + " gameId=" + game.getId()
                    + " moves=" + movesSet.size());
        } else {
            logger.error("game.import.pgn failed userId=" + ownerId);
        }
        return game != null ? game.getId() : null;
    }

    @Override
    public UUID importFromMoves(UUID ownerId, String format, String movesText, String startFen, String resultTag) {
        Position startPosition = resolveStartPosition(startFen);
        TreeSet<GameMove> moves = parseMovesByFormat(format, movesText, startPosition);

        var g = new Game();
        g.setMoves(moves);
        g.setResult(parseResult(resultTag));
        g.setUserId(ownerId);
        g = gameRepository.save(g);

        if (g != null) {
            logger.info("game.import.moves success userId=" + ownerId + " gameId=" + g.getId()
                    + " format=" + format + " moves=" + moves.size());
        } else {
            logger.error("game.import.moves failed userId=" + ownerId + " format=" + format);
        }
        return g != null ? g.getId() : null;
    }

    private Position resolveStartPosition(String startFen) {
        if (startFen != null) {
            return Position.fromFEN(startFen);
        }
        return Position.initial();
    }

    private TreeSet<GameMove> parseMovesByFormat(String format, String movesText, Position startPosition) {
        return switch (format.toLowerCase()) {
            case "uci" -> parseUciMoves(movesText, startPosition);
            case "san" -> parseSanMoves(movesText, startPosition);
            default -> throw unsupportedFormat(format);
        };
    }

    private TreeSet<GameMove> parseUciMoves(String movesText, Position startPosition) {
        Position position = startPosition;
        TreeSet<GameMove> moves = new TreeSet<>(Comparator.comparingInt(GameMove::getPlyIndex));
        int ply = 1;
        for (String uci : tokenizeUci(movesText)) {
            var move = uciNotationService.uciToMove(uci, position);
            String san = sanNotationService.moveToSan(move, position);
            position = MoveMaker.apply(position, move);
            moves.add(toGameMove(ply++, simplifySan(san), uci));
        }
        return moves;
    }

    private TreeSet<GameMove> parseSanMoves(String movesText, Position startPosition) {
        Position position = startPosition;
        TreeSet<GameMove> moves = new TreeSet<>(Comparator.comparingInt(GameMove::getPlyIndex));
        int ply = 1;
        for (String san : tokenizeSan(stripSanNoise(movesText))) {
            var move = sanNotationService.sanToMove(san, position);
            String uci = uciNotationService.moveToUci(move);
            position = MoveMaker.apply(position, move);
            moves.add(toGameMove(ply++, simplifySan(san), uci));
        }
        return moves;
    }

    private ApplicationException unsupportedFormat(String format) {
        logger.warning("game.import.moves rejected format=" + format);
        return new ApplicationException("couldn't parse game format " + format);
    }

    private static GameMove toGameMove(int ply, String san, String uci) {
        GameMove move = new GameMove();
        move.setPlyIndex(ply);
        move.setSan(san);
        move.setUci(uci);
        return move;
    }

    private static GameResult parseResult(String tag) {
        if (tag == null) return null;
        return switch (tag.trim()) {
            case "1-0" -> GameResult.WHITE_WIN;
            case "0-1" -> GameResult.BLACK_WIN;
            case "1/2-1/2" -> GameResult.DRAW;
            case "*" -> GameResult.UNFINISHED;
            default -> {
                try {
                    yield GameResult.valueOf(tag.trim());
                } catch (IllegalArgumentException ex) {
                    yield null;
                }
            }
        };
    }

    private static String stripPgnNoise(String s) {
        String tmp = BRACE_COMMENT.matcher(s).replaceAll(" ");
        tmp = Arrays.stream(tmp.split("\n"))
                .map(line -> SEMICOLON_COMMENT.matcher(line).replaceAll(" "))
                .reduce((a,b) -> a + " " + b).orElse("");
        String prev;
        do {
            prev = tmp;
            tmp = VARIATION_PARENS.matcher(tmp).replaceAll(" ");
        } while (!tmp.equals(prev));
        tmp = NAG.matcher(tmp).replaceAll(" ");
        tmp = MOVE_NUM.matcher(tmp).replaceAll(" ");
        tmp = tmp.replaceAll("\\s+", " ").trim();
        return tmp;
    }
    private static String stripSanNoise(String s) {
        return stripPgnNoise(s);
    }
    private static List<String> tokenizeSan(String cleaned) {
        if (cleaned.isBlank()) return Collections.emptyList();
        String withoutResult = RESULT_TOKEN.matcher(cleaned).replaceAll(" ").trim();
        String[] arr = withoutResult.split("\\s+");
        List<String> out = new ArrayList<>(arr.length);
        for (String t : arr) {
            String tok = t.trim();
            if (tok.isEmpty()) continue;
            if (RESULT_TOKEN.matcher(tok).matches()) continue;
            out.add(tok);
        }
        return out;
    }
    private static List<String> tokenizeUci(String text) {
        String cleaned = text.replaceAll("\\s+", " ").trim();
        if (cleaned.isBlank()) return Collections.emptyList();
        String[] arr = cleaned.split(" ");
        List<String> out = new ArrayList<>(arr.length);
        for (String tok : arr) {
            tok = tok.trim();
            if (tok.isEmpty()) continue;
            if (RESULT_TOKEN.matcher(tok).matches()) continue;
            if (tok.length() < 4) continue;
            out.add(tok);
        }
        return out;
    }
    private static String simplifySan(String san) {
        return san.replaceAll("[!?]+$", "");
    }
}
