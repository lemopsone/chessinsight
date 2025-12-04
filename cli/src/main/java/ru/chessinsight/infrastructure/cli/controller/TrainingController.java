package ru.chessinsight.infrastructure.cli.controller;

import org.springframework.stereotype.Component;
import ru.chessinsight.application.auth.service.AuthService;
import ru.chessinsight.application.game.analysis.service.AnalysisService;
import ru.chessinsight.application.game.dto.GameAnalysisDTO;
import ru.chessinsight.application.game.service.GameService;
import ru.chessinsight.application.game.training.dto.TrainingMoveRequest;
import ru.chessinsight.application.game.training.dto.TrainingMoveResponse;
import ru.chessinsight.application.game.training.service.TrainingService;
import ru.chessinsight.domain.exception.InvalidUCIException;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.repository.GameRepository;
import ru.chessinsight.domain.game.training.model.TrainingScenario;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.infrastructure.cli.exception.CliAuthRequiredException;
import ru.chessinsight.infrastructure.cli.exception.CliNotFoundException;
import ru.chessinsight.infrastructure.cli.exception.CliUsageException;

import java.util.*;

@Component
public class TrainingController implements CommandController {
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final UUID DEMO_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final TrainingService trainingService;
    private final AnalysisService analysisService;
    private final AuthService authService;
    private final GameService gameService;

    public TrainingController(TrainingService trainingService,
                              AnalysisService analysisService, AuthService authService,
                              GameService gameService) {
        this.trainingService = trainingService;
        this.analysisService = analysisService;
        this.authService = authService;
        this.gameService = gameService;
    }

    @Override public String name() { return "training"; }
    @Override public String description() { return "Manage training scenarios"; }

    @Override
    public void handle(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: training create <gameId> | list <my|demo> | start <scenarioId>");
            throw new CliUsageException("Not enough arguments.", "training create <gameId> | list <my|demo> | start <scenarioId>");
        }
        switch (args[0]) {
            case "create" -> {
                UUID gameId = UUID.fromString(args[1]);
                Game game = gameService.getGame(gameId).orElseThrow(() -> new CliNotFoundException("game with id " + gameId + " not found"));
                GameAnalysisDTO dto = analysisService.analyzeGame(game);
                List<TrainingScenario> created = trainingService.createScenariosFromAnalysis(dto);
                System.out.println("Created " + created.size() + " scenarios");
            }
            case "list" -> {
                if (args.length < 2) {
                    throw new CliUsageException("Missing scope.", "training list <my|demo>");
                }
                switch (args[1]) {
                    case "my" -> {
                        UUID userId = authService.getCurrentUser().map(User::getId).orElse(null);
                        if (userId == null) {
                            throw new CliAuthRequiredException();
                        }
                        trainingService.getNewUserScenarios(userId)
                                .forEach(s -> System.out.printf(
                                        "Scenario %s (completed=%s) game=%s%n",
                                        s.getId(), s.isCompleted(), s.getGameId()
                                ));
                    }
                    case "demo" -> {
                        trainingService.getNewUserScenarios(DEMO_USER_ID)
                                .forEach(s -> System.out.printf(
                                        "Scenario %s [DEMO] game=%s%n",
                                        s.getId(), s.getGameId()
                                ));
                    }
                    default -> throw new CliUsageException("Invalid scope " + args[1] + ".", "training list <my|demo>");
                }
            }
            case "start" -> {
                if (args.length < 2) {
                    throw new CliUsageException("Missing scenarioId.", "training start <scenarioId>");
                }
                UUID scenarioId = UUID.fromString(args[1]);
                playScenarioConsole(scenarioId);
            }
            default ->
                    throw new CliUsageException("Invalid argument " + args[0] + ".", "training create <gameId> | list <my|demo> | start <scenarioId>");
        }
    }

    public void playScenarioConsole(UUID scenarioId) {
        Optional<TrainingScenario> opt = trainingService.getScenarioById(scenarioId);
        if (opt.isEmpty()) {
            System.out.println("Scenario not found.");
            return;
        }
        TrainingScenario s = opt.get();
        UUID currentUserId = authService.getCurrentUser().map(User::getId).orElse(null);

        final boolean isDemo = (s.getUserId().equals(DEMO_USER_ID));
        if (!isDemo) {
            if (currentUserId == null || !currentUserId.equals(s.getUserId())) {
                System.out.println("You are not allowed to play this scenario.");
                return;
            }
        }
        int cursor = 0;

        System.out.println("\n--- Training Scenario ---");
        System.out.println("Game: " + s.getGameId());
        System.out.println("FEN : " + s.getPositionFEN());
        System.out.println("Type moves in SAN (e.g., Nf3) or UCI (e.g., g1f3).");
        System.out.println("Commands: ':pv' show remaining line, ':hint' next ply, ':back' exit.");

        while (true) {
            System.out.print("\nYour move > ");
            String input = SCANNER.nextLine().trim();
            if (input.isBlank()) continue;

            if (":back".equalsIgnoreCase(input)) {
                System.out.println("Exiting scenario.");
                return;
            }
            if (":pv".equalsIgnoreCase(input)) {
                printRemainingPv(s.getPvUci(), cursor);
                continue;
            }
            if (":hint".equalsIgnoreCase(input)) {
                printOneMoveHint(s.getPvUci(), cursor);
                continue;
            }

            TrainingMoveResponse resp;
            try {
                 resp = trainingService.submitMove(
                        currentUserId,
                        new TrainingMoveRequest(scenarioId, cursor, input, isDemo)
                );
            } catch (InvalidUCIException e) {
                System.out.println("The move you provided is incorrect or doesn't exist");
                continue;
            }

            if (resp.status() == TrainingMoveResponse.Status.INCORRECT) {
                System.out.println("✗ " + resp.message());
                if (resp.hintPvSan() != null && !resp.hintPvSan().isEmpty()) {
                    System.out.println("Refutation (SAN): " + String.join(" ", resp.hintPvSan()));
                } else if (resp.hintPvUci() != null && !resp.hintPvUci().isEmpty()) {
                    System.out.println("Refutation (UCI): " + String.join(" ", resp.hintPvUci()));
                }
                continue;
            }

            if (resp.acceptedMoveUci() != null && !resp.acceptedMoveUci().isBlank()) {
                System.out.println("You played: " + resp.acceptedMoveUci());
            }
            if (resp.opponentMoveUci() != null && !resp.opponentMoveUci().isBlank()) {
                System.out.println("Opponent replied: " + resp.opponentMoveUci());
            }

            cursor = resp.nextCursor();
            System.out.println((resp.status() == TrainingMoveResponse.Status.COMPLETED ? "✓ " : "→ ") + resp.message());

            if (resp.completed() || resp.status() == TrainingMoveResponse.Status.COMPLETED) {
                break;
            }
        }
    }

    private static void printRemainingPv(String pvUci, int cursor) {
        var remainingPv = remainingPv(pvUci, cursor);
        if (remainingPv != null) {
            String[] remaining = Arrays.copyOfRange(remainingPv, cursor, remainingPv.length);
            System.out.println("Remaining PV (UCI): " + String.join(" ", remaining));
        }
    }

    private static void printOneMoveHint(String pvUci, int cursor) {
        var remainingPv = remainingPv(pvUci, cursor);
        if (remainingPv != null) {
            System.out.println("Hint (next PV move): " + remainingPv[cursor]);
        }
    }

    private static String[] remainingPv(String pvUci, int cursor) {
        if (pvUci == null || pvUci.isBlank()) {
            System.out.println("(No PV available)");
            return null;
        }
        String[] all = pvUci.trim().split("\\s+");
        if (cursor >= all.length) {
            System.out.println("(PV finished)");
            return null;
        }
        return all;
    }
}
