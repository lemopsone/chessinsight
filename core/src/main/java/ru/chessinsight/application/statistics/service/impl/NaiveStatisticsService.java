package ru.chessinsight.application.statistics.service.impl;

import org.springframework.stereotype.Service;
import ru.chessinsight.application.game.analysis.service.AnalysisService;
import ru.chessinsight.application.game.dto.GameAnalysisDTO;
import ru.chessinsight.application.statistics.exception.UserNotFoundException;
import ru.chessinsight.application.statistics.service.StatisticsService;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.repository.GameRepository;
import ru.chessinsight.domain.user.model.UserStatistics;
import ru.chessinsight.domain.user.repository.UserRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class NaiveStatisticsService implements StatisticsService {
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final AnalysisService analysisService;

    public NaiveStatisticsService(UserRepository userRepository,
                                  GameRepository gameRepository,
                                  AnalysisService analysisService) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.gameRepository = Objects.requireNonNull(gameRepository);
        this.analysisService = Objects.requireNonNull(analysisService);
    }

    @Override
    public UserStatistics getUserStatistics(UUID userId) throws UserNotFoundException {
        userRepository.findOneById(userId).orElseThrow(() -> new UserNotFoundException(userId.toString()));

        List<Game> games = gameRepository.findAllByUserId(userId);

        double sumAll = 0.0;
        int cntAll = 0;

        double sumWhite = 0.0;
        int cntWhite = 0;

        double sumBlack = 0.0;
        int cntBlack = 0;

        for (Game g : games) {
            GameAnalysisDTO dto = analysisService.analyzeGame(g);

            int best = size(dto.bestMoves());
            int good = size(dto.goodMoves());
            int inacc = size(dto.inaccuracies());
            int mistake = size(dto.mistakes());
            int blunder = size(dto.blunders());

            int total = best + good + inacc + mistake + blunder;
            if (total == 0) continue;

            double gameAcc = ((double) (best + good)) / total;
            sumAll += gameAcc;
            cntAll += 1;


            sumWhite += gameAcc;
            cntWhite += 1;
            sumBlack += gameAcc;
            cntBlack += 1;
        }

        double accAll = cntAll > 0 ? sumAll / cntAll : 0.0;
        double accWhite = cntWhite > 0 ? sumWhite / cntWhite : 0.0;
        double accBlack = cntBlack > 0 ? sumBlack / cntBlack : 0.0;

        return new UserStatistics(accAll, accWhite, accBlack);
    }

    private static int size(List<?> list) {
        return list == null ? 0 : list.size();
    }
}
