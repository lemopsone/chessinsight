package ru.chessinsight.application.game.analysis.service;

import ru.chessinsight.application.game.analysis.model.MoveCategory;

public interface AccuracyCalculationService {
    double moveWeight(long moveNum);
    double movePenalty(MoveCategory category, long moveNum);
    double calculateAccuracy(double totalPenalty, double totalWeight);
}
