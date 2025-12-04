package ru.chessinsight.infrastructure.cli.controller;

import org.springframework.stereotype.Component;
import ru.chessinsight.application.game.analysis.service.AnalysisService;
import ru.chessinsight.application.game.dto.GameAnalysisDTO;
import ru.chessinsight.application.game.service.GameService;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.repository.GameRepository;
import ru.chessinsight.infrastructure.cli.exception.CliNotFoundException;
import ru.chessinsight.infrastructure.cli.exception.CliUsageException;

import java.util.UUID;

@Component
public class AnalysisController implements CommandController {
    private final AnalysisService analysisService;
    private final GameService gameService;

    public AnalysisController(AnalysisService analysisService, GameService gameService) {
        this.analysisService = analysisService;
        this.gameService = gameService;
    }

    @Override public String name() { return "analyze"; }
    @Override public String description() { return "Analyze games"; }

    @Override
    public void handle(String[] args) {
        if (args.length < 1) {
            throw new CliUsageException("Not enough arguments", "analyze <gameId>");
        }
        UUID gid = UUID.fromString(args[0]);
        Game g = gameService.getGame(gid).orElseThrow(() -> new CliNotFoundException("Game not found"));
        GameAnalysisDTO dto = analysisService.analyzeGame(g);
        System.out.printf("Game %s: WhiteAcc=%.1f BlackAcc=%.1f%n",
                dto.gameId(), dto.accuracyWhite(), dto.accuracyBlack());
    }
}
