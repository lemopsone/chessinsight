package ru.chessinsight.infrastructure.cli.controller;

import org.springframework.stereotype.Component;
import ru.chessinsight.infrastructure.cli.api.WebApiClient;
import ru.chessinsight.infrastructure.cli.api.dto.ApiGameAnalysis;
import ru.chessinsight.infrastructure.cli.api.dto.ApiMoveAnalysis;
import ru.chessinsight.infrastructure.cli.exception.CliUsageException;

import java.util.UUID;

@Component
public class AnalysisController implements CommandController {

    private final WebApiClient api;

    public AnalysisController(WebApiClient api) {
        this.api = api;
    }

    @Override public String name() { return "analysis"; }
    @Override public String description() { return "Game and move analysis via web API"; }

    @Override
    public void handle(String[] args) {
        if (args.length < 1) {
            throw new CliUsageException("Missing subcommand.",
                    "analysis game <gameId>\n" +
                            "analysis move <fen> <uci> [san]");
        }
        switch (args[0].toLowerCase()) {
            case "game" -> handleGame(args);
            case "move" -> handleMove(args);
            default -> throw new CliUsageException("Unknown subcommand.",
                    "analysis game <gameId>\n" +
                            "analysis move <fen> <uci> [san]");
        }
    }

    private void handleGame(String[] args) {
        if (args.length < 2) {
            throw new CliUsageException("Not enough arguments.",
                    "analysis game <gameId>");
        }
        UUID gameId = UUID.fromString(args[1]);

        ApiGameAnalysis a = api.analyzeGame(gameId);
        System.out.printf("Game %s analysis:%n", a.getGameId());
        System.out.printf("Accuracy white: %.2f%%%n", a.getAccuracyWhite());
        System.out.printf("Accuracy black: %.2f%%%n", a.getAccuracyBlack());
        System.out.printf("Best moves: %d, inaccuracies: %d, mistakes: %d, blunders: %d%n",
                size(a.getBestMoves()), size(a.getInaccuracies()), size(a.getMistakes()), size(a.getBlunders()));
    }

    private void handleMove(String[] args) {
        if (args.length < 3) {
            throw new CliUsageException("Not enough arguments.",
                    "analysis move <fen> <uci> [san]");
        }
        String fen = args[1];
        String uci = args[2];
        String san = args.length >= 4 ? args[3] : null;

        ApiMoveAnalysis a = api.analyzeMove(fen, san, uci, null);
        System.out.println("Position: " + a.getPositionFEN());
        System.out.println("Best move: " + a.getBestMoveSan() + " (" + a.getBestMoveUci() + ")");
        System.out.println("Eval best: " + a.getBestMoveEval());
        System.out.println("Eval player: " + a.getPlayerMoveEval());
        if (a.getMateScore() != null) {
            System.out.println("Mate score: " + a.getMateScore());
        }
    }

    private static int size(java.util.List<?> list) {
        return list == null ? 0 : list.size();
    }
}
