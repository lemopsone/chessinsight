package ru.chessinsight.application.game.analysis.service.impl;

import org.springframework.stereotype.Service;
import ru.chessinsight.application.game.analysis.model.MoveCategory;
import ru.chessinsight.application.game.analysis.service.AccuracyCalculationService;

@Service
public class SimpleAccuracyCalculationService implements AccuracyCalculationService {
    private static final double OPENING_WEIGHT = 0.8;
    private static final double MIDDLEGAME_WEIGHT = 1;
    private static final double ENDGAME_WEIGHT = 1.2;


    @Override
    public double moveWeight(long moveNum) {
        if (moveNum <= 10) {
            return OPENING_WEIGHT;
        } else if (moveNum <= 40) {
            return MIDDLEGAME_WEIGHT;
        } else {
            return ENDGAME_WEIGHT;
        }
    }

    @Override
    public double movePenalty(MoveCategory category, long moveNum) {
        return switch (category) {
            case BEST_MOVE, FORCED_MATE -> 0;
            case GOOD_MOVE -> 10;
            case INACCURACY -> 30;
            case MISTAKE -> 60;
            case BLUNDER -> 90;
        };
    }

    @Override
    public double calculateAccuracy(double totalPenalty, double totalWeight) {
        double baseAccuracy = Math.max(0, 100 - (totalPenalty / totalWeight));
        return Math.pow(baseAccuracy / 100.0, 0.7) * 100.0;
    }
}
