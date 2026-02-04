package ru.chessinsight.application.game.analysis.service.impl;

import org.junit.jupiter.api.Test;
import ru.chessinsight.application.game.analysis.model.MoveCategory;
import ru.chessinsight.application.game.dto.MoveAnalysisDTO;
import ru.chessinsight.application.game.dto.MoveDTO;
import io.qameta.allure.Tag;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class SimpleMoveClassificationServiceTest {
    private final SimpleMoveClassificationService service = new SimpleMoveClassificationService();

    @Test
    void classifyMove_returnsForcedMate_whenMateScorePresent() {
        MoveAnalysisDTO dto = new MoveAnalysisDTO("fen", new MoveDTO(1L, "fen", "e4", "e2e4"), 0.0, 0.0, 1);

        MoveCategory result = service.classifyMove(dto);

        assertEquals(MoveCategory.FORCED_MATE, result);
    }

    @Test
    void classifyMove_returnsBlunder_whenDeltaLarge() {
        MoveAnalysisDTO dto = new MoveAnalysisDTO("fen", new MoveDTO(1L, "fen", "e4", "e2e4"), 5.0, 0.0, null);

        MoveCategory result = service.classifyMove(dto);

        assertEquals(MoveCategory.BLUNDER, result);
    }
}