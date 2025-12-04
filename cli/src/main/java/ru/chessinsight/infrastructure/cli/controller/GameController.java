package ru.chessinsight.infrastructure.cli.controller;

import org.springframework.stereotype.Component;
import ru.chessinsight.application.auth.service.AuthService;
import ru.chessinsight.application.game.service.GameImportService;
import ru.chessinsight.application.game.service.GameService;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.repository.GameRepository;
import ru.chessinsight.domain.user.model.Role;
import ru.chessinsight.infrastructure.cli.exception.CliAuthRequiredException;
import ru.chessinsight.infrastructure.cli.exception.CliException;
import ru.chessinsight.infrastructure.cli.exception.CliNotFoundException;
import ru.chessinsight.infrastructure.cli.exception.CliUsageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Component
public class GameController implements CommandController {

    private final AuthService authService;
    private final GameImportService importService;
    private final GameService gameService;

    private static final UUID DEMO_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    public GameController(AuthService authService,
                          GameImportService importService, GameService gameService) {
        this.authService = authService;
        this.importService = importService;
        this.gameService = gameService;
    }

    @Override public String name() { return "game"; }
    @Override public String description() { return "List or import games"; }

    @Override
    public void handle(String[] args) {
        if (args.length == 0) {
            throw new CliUsageException("Missing subcommand.",
                    "game list {demo|my} | game import {demo|my} {pgn|san|uci} <payload|@file> [--fen <FEN>] [--result <res>]");
        }
        switch (args[0].toLowerCase()) {
            case "list" -> handleList(args);
            case "import" -> handleImport(args);
            default -> throw new CliUsageException("Unknown subcommand: " + args[0],
                    "game list {demo|my} | game import {demo|my} {pgn|san|uci} <payload|@file> [--fen <FEN>] [--result <res>]");
        }
    }

    private void handleList(String[] args) {
        if (args.length < 1 || !"list".equalsIgnoreCase(args[0])) {
            throw new CliUsageException("Unknown subcommand.", "game list {demo|my}");
        }
        if (args.length < 2) {
            throw new CliUsageException("Missing argument.", "game list {demo|my}");
        }

        List<Game> games = switch (args[1].toLowerCase()) {
            case "demo" -> gameService.findDemoGames();
            case "my" -> {
                UUID uid = authService.getCurrentUserId()
                        .orElseThrow(CliAuthRequiredException::new);
                yield gameService.findUserGames(uid);
            }
            default -> throw new CliUsageException("Unknown option: " + args[1], "game list {demo|my}");
        };

        if (games.isEmpty()) {
            System.out.println("(no games)");
            return;
        }

        games.forEach(g -> System.out.printf(" - %s (moves=%d)%n",
                g.getId(), g.getMoves() != null ? g.getMoves().size() : 0));
    }

    private void handleImport(String[] args) {
        if (args.length < 3) {
            throw new CliUsageException("Not enough arguments.",
                    "game import {demo|my} {pgn|san|uci} <payload|@file> [--fen <FEN>] [--result <res>]");
        }

        UUID ownerId = resolveOwner(args[1]);
        if (args[1].equalsIgnoreCase("demo")) {
            var user = authService.getCurrentUser();
            if (user.isEmpty() || !user.get().getRoles().contains(Role.ROLE_ADMIN))
                throw new CliException("Only admins can upload demo games");
        }
        String format = args[2].toUpperCase();

        if (!format.equals("PGN") && !format.equals("SAN") && !format.equals("UCI")) {
            throw new CliUsageException("Unknown format: " + args[2], "use: pgn | san | uci");
        }

        if (args.length < 4) {
            throw new CliUsageException("Missing payload or @file.",
                    "game import {demo|my} {pgn|san|uci} <payload|@file> [--fen <FEN>] [--result <res>]");
        }

        String payloadOrFile = args[3];
        String content = payloadOrFile.startsWith("@")
                ? readFileOrFail(payloadOrFile.substring(1))
                : joinRest(args, 3);

        String fen = null;
        String result = null;
        for (int i = 4; i < args.length; i++) {
            String a = args[i];
            if ("--fen".equalsIgnoreCase(a) && i + 1 < args.length) {
                fen = args[++i];
            } else if ("--result".equalsIgnoreCase(a) && i + 1 < args.length) {
                result = args[++i];
            } else {
                throw new CliUsageException("Unknown option: " + a,
                        "game import {demo|my} {pgn|san|uci} <payload|@file> [--fen <FEN>] [--result <res>]");
            }
        }

        UUID gameId;
        switch (format) {
            case "PGN" -> gameId = importService.importFromPgn(ownerId, content, fen, result);
            case "SAN", "UCI" -> gameId = importService.importFromMoves(ownerId, format, content, fen, result);
            default -> throw new CliUsageException("Unsupported format: " + format, "pgn | san | uci");
        }

        System.out.printf("Game imported successfully. id=%s%n", gameId);
    }

    private UUID resolveOwner(String who) {
        return switch (who.toLowerCase()) {
            case "demo" -> DEMO_USER_ID;
            case "my" -> authService.getCurrentUserId()
                    .orElseThrow(CliAuthRequiredException::new);
            default -> throw new CliUsageException("Unknown owner: " + who, "{demo|my}");
        };
    }

    private static String readFileOrFail(String path) {
        try {
            return Files.readString(Path.of(path));
        } catch (IOException e) {
            throw new CliNotFoundException("Cannot read file: " + path);
        }
    }

    private static String joinRest(String[] args, int fromIdx) {
        StringBuilder sb = new StringBuilder();
        for (int i = fromIdx; i < args.length; i++) {
            if (args[i].startsWith("--")) break;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }
}
