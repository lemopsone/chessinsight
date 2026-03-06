package ru.chessinsight.infrastructure.cli.controller;

import org.springframework.stereotype.Component;
import ru.chessinsight.infrastructure.cli.api.WebApiClient;
import ru.chessinsight.infrastructure.cli.api.dto.ApiGameSummary;
import ru.chessinsight.infrastructure.cli.api.dto.ApiPageResponse;
import ru.chessinsight.infrastructure.cli.exception.CliNotFoundException;
import ru.chessinsight.infrastructure.cli.exception.CliUsageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class GameController implements CommandController {

    private final WebApiClient api;

    public GameController(WebApiClient api) {
        this.api = api;
    }

    @Override public String name() { return "game"; }
    @Override public String description() { return "List or import games"; }

    @Override
    public void handle(String[] args) {
        if (args.length == 0) {
            throw new CliUsageException("Missing subcommand.",
                    "game list [page] [size]\n" +
                            "game import pgn <payload|@file> [--fen <FEN>] [--result <res>]");
        }
        switch (args[0].toLowerCase()) {
            case "list"   -> handleList(args);
            case "import" -> handleImport(args);
            default -> throw new CliUsageException("Unknown subcommand: " + args[0],
                    "game list [page] [size]\n" +
                            "game import pgn <payload|@file> [--fen <FEN>] [--result <res>]");
        }
    }

    private void handleList(String[] args) {
        int page = 0;
        int size = 20;
        if (args.length >= 2) page = Integer.parseInt(args[1]);
        if (args.length >= 3) size = Integer.parseInt(args[2]);

        ApiPageResponse<ApiGameSummary> resp = api.listMyGames(page, size);
        if (resp.getContent() == null || resp.getContent().isEmpty()) {
            System.out.println("(no games)");
            return;
        }

        System.out.printf("Page %d / %d (total=%d)%n",
                resp.getPage() + 1, resp.getTotalPages(), resp.getTotalElements());

        for (ApiGameSummary g : resp.getContent()) {
            System.out.printf(" - %s  %s  %s vs %s  result=%s  moves=%d%n",
                    g.getId(),
                    g.getDate(),
                    nullToDash(g.getWhiteName()),
                    nullToDash(g.getBlackName()),
                    nullToDash(g.getResult()),
                    g.getMovesCount() != null ? g.getMovesCount() : 0);
        }
        if (resp.isHasNext()) {
            System.out.println("... has more pages, use: game list " + (resp.getPage() + 1));
        }
    }

    private void handleImport(String[] args) {
        if (args.length < 3) {
            throw new CliUsageException("Not enough arguments.",
                    "game import pgn <payload|@file> [--fen <FEN>] [--result <res>]");
        }
        String format = args[1].toUpperCase();
        if (!"PGN".equals(format)) {
            throw new CliUsageException("Only PGN import is supported via web API.",
                    "game import pgn <payload|@file> [--fen <FEN>] [--result <res>]");
        }

        String payload;
        if (args[2].startsWith("@")) {
            payload = readFileOrFail(args[2].substring(1));
        } else {
            payload = args[2];
        }

        String fen = findImportOptionValue(args, "--fen");
        String resultTag = findImportOptionValue(args, "--result");

        ApiGameSummary game = api.importGameFromPgn(payload, fen, resultTag);
        System.out.println("Imported game: " + game.getId());
    }

    private static String findImportOptionValue(String[] args, String optionName) {
        int optionIndex = indexOfOption(args, optionName);
        if (optionIndex < 0) {
            return null;
        }
        return optionIndex + 1 < args.length ? args[optionIndex + 1] : null;
    }

    private static int indexOfOption(String[] args, String optionName) {
        for (int i = 3; i < args.length; i++) {
            if (optionName.equals(args[i])) {
                return i;
            }
        }
        return -1;
    }

    private static String readFileOrFail(String path) {
        try {
            return Files.readString(Path.of(path));
        } catch (IOException e) {
            throw new CliNotFoundException("Cannot read file: " + path);
        }
    }

    private static String nullToDash(String s) {
        return s == null ? "-" : s;
    }
}
