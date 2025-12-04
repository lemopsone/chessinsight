package ru.chessinsight.application.game.service.impl;

import org.springframework.stereotype.Service;
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

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    public ConcreteGameImportService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
        this.pgnService = new PgnService();
        this.uciNotationService = new UciNotationService();
        this.sanNotationService = new SanNotationService();
    }

    @Override
    public UUID importFromPgn(UUID ownerId, String pgn, String startFen, String resultTag) {
        var game = pgnService.createEmptyGameFromPGN(pgn);
        var moves = pgnService.extractMovesFromGameMainline(game);
        var movesSet = new TreeSet<>(Comparator.comparing(GameMove::getPlyIndex));
        movesSet.addAll(moves);
        game.setMoves(movesSet);
        game.setUserId(ownerId);
        game = gameRepository.save(game);

        return game != null ? game.getId() : null;
    }

    @Override
    public UUID importFromMoves(UUID ownerId, String format, String movesText, String startFen, String resultTag) {
        Position pos;
        if (startFen != null) {
            pos = Position.fromFEN(startFen);
        } else {
            pos = Position.initial();
        }
        var moves = new TreeSet<>(Comparator.comparingInt(GameMove::getPlyIndex));
        int ply = 1;
        switch (format.toLowerCase()) {
            case "uci":
                for (String uci : tokenizeUci(movesText)) {
                    var move = uciNotationService.uciToMove(uci, pos);
                    String san = sanNotationService.moveToSan(move, pos);
                    pos = MoveMaker.apply(pos, move);
                    GameMove m = new GameMove();
                    m.setPlyIndex(ply++);
                    m.setUci(uci);
                    m.setSan(simplifySan(san));
                    moves.add(m);
                }
                break;
            case "san":
                for (String san : tokenizeSan(stripSanNoise(movesText))) {
                    var move = sanNotationService.sanToMove(san, pos);
                    String uci = uciNotationService.moveToUci(move);
                    MoveMaker.apply(pos, move);
                    GameMove m = new GameMove();
                    m.setPlyIndex(ply++);
                    m.setSan(simplifySan(san));
                    m.setUci(uci);
                    moves.add(m);
                }
                break;
            default: throw new ApplicationException("couldn't parse game format " + format);
        }
        var g = new Game();
        g.setMoves(moves);
        g.setResult(resultTag != null ? GameResult.valueOf(resultTag) : null);
        g.setUserId(ownerId);
        g = gameRepository.save(g);
        return g != null ? g.getId() : null;
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
