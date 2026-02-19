package ru.chessinsight.testutil;

import io.qameta.allure.Allure;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Locale;

public class AllureStageExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        String stage = detectStage(context.getRequiredTestClass().getSimpleName());
        String stageTitle = stageTitle(stage);

        Allure.label("epic", "Test Stages");
        Allure.label("feature", stageTitle);
        Allure.label("tag", stage);
        Allure.label("parentSuite", stageTitle);
    }

    private static String detectStage(String className) {
        String explicit = System.getProperty("allure.test.stage", "").trim().toLowerCase(Locale.ROOT);
        if ("unit".equals(explicit) || "integration".equals(explicit) || "e2e".equals(explicit)) {
            return explicit;
        }
        if (className.endsWith("E2EIT")) {
            return "e2e";
        }
        if (className.endsWith("IT") || className.endsWith("ITCase")) {
            return "integration";
        }
        return "unit";
    }

    private static String stageTitle(String stage) {
        return switch (stage) {
            case "integration" -> "Integration";
            case "e2e" -> "E2E";
            default -> "Unit";
        };
    }
}
