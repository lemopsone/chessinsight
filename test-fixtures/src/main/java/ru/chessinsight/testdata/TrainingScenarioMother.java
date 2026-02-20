package ru.chessinsight.testdata;

import ru.chessinsight.domain.game.training.model.TrainingScenario;

public final class TrainingScenarioMother {
    private TrainingScenarioMother() {}

    public static TrainingScenario completedScenario() {
        return TrainingScenarioBuilder.scenario().withCompleted(true).build();
    }

    public static TrainingScenario pendingScenario() {
        return TrainingScenarioBuilder.scenario().withCompleted(false).build();
    }
}
