package ru.chessinsight.infrastructure.persistence.jpa.mapper;

import org.junit.jupiter.api.Test;
import ru.chessinsight.domain.game.model.GameAnalysis;
import ru.chessinsight.infrastructure.persistence.jpa.model.GameAnalysisEntity;
import ru.chessinsight.testdata.GameAnalysisEntityBuilder;

import java.time.OffsetDateTime;
import ru.chessinsight.testutil.Tag;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class GameAnalysisMapperTest {
    private final GameAnalysisMapper mapper = new GameAnalysisMapper();

    @Test
    void toDomain_mapsFields() {
        GameAnalysisEntity entity = GameAnalysisEntityBuilder.analysisEntity()
                .withAccuracyWhite(0.9)
                .withAccuracyBlack(0.8)
                .withInaccuracies(2)
                .withMistakes(1)
                .withBlunders(0)
                .withAnalyzedAt(OffsetDateTime.parse("2024-03-01T10:00:00Z"))
                .build();

        GameAnalysis result = mapper.toDomain(entity);

        assertEquals(0.9, result.getAccuracyWhite());
        assertEquals(0.8, result.getAccuracyBlack());
        assertEquals(2, result.getInaccuracies());
        assertEquals(1, result.getMistakes());
        assertEquals(0, result.getBlunders());
        assertEquals(OffsetDateTime.parse("2024-03-01T10:00:00Z"), result.getAnalyzedAt());
    }

    @Test
    void toDomain_returnsNull_whenEntityNull() {
        GameAnalysis result = mapper.toDomain(null);

        assertNull(result);
    }

    @Test
    void toEntity_mapsFields() {
        GameAnalysis analysis = new GameAnalysis();
        analysis.setAccuracyWhite(0.7);
        analysis.setAccuracyBlack(0.6);
        analysis.setInaccuracies(3);
        analysis.setMistakes(2);
        analysis.setBlunders(1);
        analysis.setAnalyzedAt(OffsetDateTime.parse("2024-04-01T10:00:00Z"));

        GameAnalysisEntity result = mapper.toEntity(analysis);

        assertEquals(0.7, result.getAccuracyWhite());
        assertEquals(0.6, result.getAccuracyBlack());
        assertEquals(3, result.getInaccuracies());
        assertEquals(2, result.getMistakes());
        assertEquals(1, result.getBlunders());
        assertEquals(OffsetDateTime.parse("2024-04-01T10:00:00Z"), result.getAnalyzedAt());
    }

    @Test
    void toEntity_returnsNull_whenDomainNull() {
        GameAnalysisEntity result = mapper.toEntity(null);

        assertNull(result);
    }
}