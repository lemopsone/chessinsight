package ru.chessinsight.domain.game.training.model;

import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.UUID;
import ru.chessinsight.testutil.Tag;
import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class TrainingScenarioTest {

    @Test
    void setters_and_getters_work() {
        TrainingScenario t = new TrainingScenario();
        UUID id = UUID.randomUUID();
        t.setId(id);
        t.setUserId(UUID.randomUUID());
        t.setGameId(UUID.randomUUID());
        t.setPositionFEN("8/8/8/8/8/8/8/8 w - - 0 1");
        t.setPvSan("Qh5+ Nc6");
        t.setPvUci("e2e4 e7e5");
        t.setPrompt("Find the best move");
        t.setCompleted(true);
        OffsetDateTime now = OffsetDateTime.now();
        t.setCompletedAt(now);

        assertEquals(id, t.getId());
        assertEquals("Qh5+ Nc6", t.getPvSan());
        assertTrue(t.isCompleted());
        assertEquals(now, t.getCompletedAt());
    }
}