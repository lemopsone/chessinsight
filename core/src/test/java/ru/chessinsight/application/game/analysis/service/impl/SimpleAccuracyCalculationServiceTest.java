package ru.chessinsight.application.game.analysis.service.impl;

import org.junit.jupiter.api.Test;
import ru.chessinsight.application.game.analysis.model.MoveCategory;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class SimpleAccuracyCalculationServiceTest {
    private final SimpleAccuracyCalculationService service = new SimpleAccuracyCalculationService();

    @Test
    void moveWeight_returnsOpeningWeight() {
        double w = service.moveWeight(5);

        assertEquals(0.8, w);
    }

    @Test
    void moveWeight_returnsEndgameWeight() {
        double w = service.moveWeight(50);

        assertEquals(1.2, w);
    }

    @Test
    void movePenalty_returnsForGoodMove() {
        double p = service.movePenalty(MoveCategory.GOOD_MOVE, 12);

        assertEquals(10, p);
    }

    @Test
    void movePenalty_returnsForBlunder() {
        double p = service.movePenalty(MoveCategory.BLUNDER, 12);

        assertEquals(90, p);
    }

    @Test
    void calculateAccuracy_returns100_whenNoPenalty() {
        double acc = service.calculateAccuracy(0, 1);

        assertEquals(100.0, acc);
    }

    @Test
    void calculateAccuracy_decreases_whenPenaltyHigh() {
        double acc = service.calculateAccuracy(200, 1);

        assertTrue(acc < 100.0);
    }
}