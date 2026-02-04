package ru.chessinsight.infrastructure.persistence.jpa.mapper;

import org.junit.jupiter.api.Test;
import ru.chessinsight.domain.game.training.model.TrainingScenario;
import ru.chessinsight.infrastructure.persistence.jpa.model.TrainingScenarioEntity;
import ru.chessinsight.testdata.TrainingScenarioEntityBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;
import io.qameta.allure.Tag;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class TrainingScenarioMapperTest {
    private final TrainingScenarioMapper mapper = new TrainingScenarioMapper();

    @Test
    void toDomain_mapsFields() {
        TrainingScenarioEntity entity = TrainingScenarioEntityBuilder.scenarioEntity()
                .withId(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"))
                .withUserId(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"))
                .withGameId(UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"))
                .withPositionFEN("fen")
                .withPvSan("e4 e5")
                .withPvUci("e2e4 e7e5")
                .withPrompt("prompt")
                .withCompleted(true)
                .withCompletedAt(OffsetDateTime.parse("2024-05-01T10:00:00Z"))
                .build();

        TrainingScenario result = mapper.toDomain(entity);

        assertEquals(entity.getId(), result.getId());
        assertEquals(entity.getUserId(), result.getUserId());
        assertEquals(entity.getGameId(), result.getGameId());
        assertEquals("fen", result.getPositionFEN());
        assertEquals("e4 e5", result.getPvSan());
        assertEquals("e2e4 e7e5", result.getPvUci());
        assertEquals("prompt", result.getPrompt());
        assertTrue(result.isCompleted());
        assertEquals(OffsetDateTime.parse("2024-05-01T10:00:00Z"), result.getCompletedAt());
    }

    @Test
    void toDomain_returnsNull_whenEntityNull() {
        TrainingScenario result = mapper.toDomain(null);

        assertNull(result);
    }

    @Test
    void toEntity_mapsFields() {
        TrainingScenario scenario = new TrainingScenario(
                UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"),
                UUID.fromString("11111111-2222-3333-4444-555555555555"),
                UUID.fromString("66666666-7777-8888-9999-aaaaaaaaaaaa"),
                "fen",
                "e4",
                "e2e4",
                "prompt",
                false,
                null
        );

        TrainingScenarioEntity result = mapper.toEntity(scenario);

        assertEquals(scenario.getId(), result.getId());
        assertEquals(scenario.getUserId(), result.getUserId());
        assertEquals(scenario.getGameId(), result.getGameId());
        assertEquals("fen", result.getPositionFEN());
        assertEquals("e4", result.getPvSan());
        assertEquals("e2e4", result.getPvUci());
        assertEquals("prompt", result.getPrompt());
        assertFalse(result.isCompleted());
        assertNull(result.getCompletedAt());
    }

    @Test
    void toEntity_returnsNull_whenDomainNull() {
        TrainingScenarioEntity result = mapper.toEntity(null);

        assertNull(result);
    }
}