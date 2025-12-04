package ru.chessinsight.infrastructure.cli.controller;

import org.springframework.stereotype.Component;
import ru.chessinsight.infrastructure.cli.api.WebApiClient;
import ru.chessinsight.infrastructure.cli.api.dto.ApiPageResponse;
import ru.chessinsight.infrastructure.cli.api.dto.ApiTrainingMoveResponse;
import ru.chessinsight.infrastructure.cli.api.dto.ApiTrainingScenario;
import ru.chessinsight.infrastructure.cli.exception.CliUsageException;

import java.util.UUID;

@Component
public class TrainingController implements CommandController {

    private final WebApiClient api;

    public TrainingController(WebApiClient api) {
        this.api = api;
    }

    @Override public String name() { return "training"; }
    @Override public String description() { return "Training scenarios"; }

    @Override
    public void handle(String[] args) {
        if (args.length == 0) {
            throw new CliUsageException("Missing subcommand.",
                    "training list [completed] [page] [size]\n" +
                            "training step <scenarioId> <moveUci> [cursor] [--demo]");
        }
        switch (args[0].toLowerCase()) {
            case "list" -> handleList(args);
            case "step" -> handleStep(args);
            default -> throw new CliUsageException("Unknown subcommand.",
                    "training list [completed] [page] [size]\n" +
                            "training step <scenarioId> <moveUci> [cursor] [--demo]");
        }
    }

    private void handleList(String[] args) {
        Boolean completed = null;
        int page = 0;
        int size = 20;

        if (args.length >= 2 && !"null".equalsIgnoreCase(args[1])) {
            completed = Boolean.parseBoolean(args[1]);
        }
        if (args.length >= 3) page = Integer.parseInt(args[2]);
        if (args.length >= 4) size = Integer.parseInt(args[3]);

        ApiPageResponse<ApiTrainingScenario> resp = api.listTrainingScenarios(completed, page, size);
        if (resp.getContent() == null || resp.getContent().isEmpty()) {
            System.out.println("(no scenarios)");
            return;
        }

        System.out.printf("Page %d / %d (total=%d)%n",
                resp.getPage() + 1, resp.getTotalPages(), resp.getTotalElements());
        for (ApiTrainingScenario s : resp.getContent()) {
            System.out.printf(" - %s | game=%s | completed=%s | FEN=%s%n",
                    s.getId(),
                    s.getGameId(),
                    s.isCompleted(),
                    s.getPositionFEN());
        }
    }

    private void handleStep(String[] args) {
        if (args.length < 3) {
            throw new CliUsageException("Not enough arguments.",
                    "training step <scenarioId> <moveUci> [cursor] [--demo]");
        }

        UUID scenarioId = UUID.fromString(args[1]);
        String moveUci = args[2];
        Integer cursor = null;
        boolean demo = false;

        if (args.length >= 4 && !args[3].startsWith("--")) {
            cursor = Integer.parseInt(args[3]);
        }
        for (int i = 4; i < args.length; i++) {
            if ("--demo".equals(args[i])) {
                demo = true;
                break;
            }
        }

        ApiTrainingScenario scenario = api.getTrainingScenario(scenarioId);
        System.out.printf("Scenario %s | game=%s%n", scenario.getId(), scenario.getGameId());
        System.out.println("Position: " + scenario.getPositionFEN());
        System.out.println("Pv: " + scenario.getPvSan());

        ApiTrainingMoveResponse resp = api.submitTrainingMove(scenarioId, cursor, moveUci, demo);

        System.out.printf("Status: %s%n", resp.getStatus());
        if (resp.getMessage() != null) {
            System.out.println("Message: " + resp.getMessage());
        }
        if (resp.getAcceptedMoveUci() != null) {
            System.out.println("Accepted move: " + resp.getAcceptedMoveUci());
        }
        if (resp.getOpponentMoveUci() != null) {
            System.out.println("Opponent move: " + resp.getOpponentMoveUci());
        }
        if (resp.getNextCursor() != null) {
            System.out.println("Next cursor: " + resp.getNextCursor());
        }
        if (resp.getHintPvSan() != null) {
            System.out.println("Hint: " + resp.getHintPvSan());
        }
    }
}
