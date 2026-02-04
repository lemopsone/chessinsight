package ru.chessinsight.infrastructure.persistence.jpa.mapper;

import org.junit.jupiter.api.Test;
import ru.chessinsight.application.game.analysis.model.MoveCategory;
import ru.chessinsight.domain.game.model.GameMove;
import ru.chessinsight.domain.game.model.GameMoveAnalysis;
import ru.chessinsight.infrastructure.persistence.jpa.model.GameMoveEntity;
import ru.chessinsight.testdata.GameMoveEntityBuilder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GameMoveMapperTest {
    private final GameMoveMapper mapper = new GameMoveMapper();

    @Test
    void toDomain_mapsFields_withAnalysis() {
        GameMoveEntity entity = GameMoveEntityBuilder.moveEntity()
                .withId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .withPlyIndex(3)
                .withSan("Nf3")
                .withUci("g1f3")
                .withPositionFEN("fen")
                .withCommentBefore("c1")
                .withCommentAfter("c2")
                .withAnalysisEvalCp(0.3)
                .withAnalysisMateScore(null)
                .withAnalysisBestUci("g1f3")
                .withAnalysisCpLoss(0.2)
                .withAnalysisCategory("GOOD_MOVE")
                .build();

        GameMove result = mapper.toDomain(entity);

        assertEquals(entity.getId(), result.getId());
        assertEquals(3, result.getPlyIndex());
        assertEquals("Nf3", result.getSan());
        assertEquals("g1f3", result.getUci());
        assertEquals("fen", result.getPositionFEN());
        assertEquals("c1", result.getCommentBefore());
        assertEquals("c2", result.getCommentAfter());
        assertNotNull(result.getAnalysis());
        assertEquals(0.3, result.getAnalysis().evalCp());
        assertEquals(0.2, result.getAnalysis().cpLoss());
        assertEquals(MoveCategory.GOOD_MOVE, result.getAnalysis().category());
    }

    @Test
    void toDomain_returnsNull_whenEntityNull() {
        GameMove result = mapper.toDomain(null);

        assertNull(result);
    }

    @Test
    void toEntity_mapsFields_withAnalysis() {
        GameMoveAnalysis analysis = new GameMoveAnalysis(0.4, 1, "e2e4", 0.1, MoveCategory.MISTAKE);
        GameMove move = new GameMove(
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                5,
                "e4",
                "e2e4",
                "fen",
                "before",
                "after",
                analysis
        );

        GameMoveEntity result = mapper.toEntity(move);

        assertEquals(move.getId(), result.getId());
        assertEquals(5, result.getPlyIndex());
        assertEquals("e4", result.getSan());
        assertEquals("e2e4", result.getUci());
        assertEquals("fen", result.getPositionFEN());
        assertEquals("before", result.getCommentBefore());
        assertEquals("after", result.getCommentAfter());
        assertEquals(0.4, result.getAnalysisEvalCp());
        assertEquals(1, result.getAnalysisMateScore());
        assertEquals("e2e4", result.getAnalysisBestUci());
        assertEquals(0.1, result.getAnalysisCpLoss());
        assertEquals("MISTAKE", result.getAnalysisCategory());
    }

    @Test
    void toEntity_returnsNull_whenDomainNull() {
        GameMoveEntity result = mapper.toEntity(null);

        assertNull(result);
    }
}