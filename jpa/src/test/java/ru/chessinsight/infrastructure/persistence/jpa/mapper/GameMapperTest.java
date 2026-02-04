package ru.chessinsight.infrastructure.persistence.jpa.mapper;

import org.junit.jupiter.api.Test;
import ru.chessinsight.application.game.analysis.model.MoveCategory;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.model.GameMove;
import ru.chessinsight.domain.game.model.GameMoveAnalysis;
import ru.chessinsight.domain.game.model.GameResult;
import ru.chessinsight.infrastructure.persistence.jpa.model.GameEntity;
import ru.chessinsight.infrastructure.persistence.jpa.model.GameMoveEntity;
import ru.chessinsight.testdata.GameBuilder;
import ru.chessinsight.testdata.GameEntityMother;
import ru.chessinsight.testdata.GameMoveBuilder;
import ru.chessinsight.testdata.GameMoveEntityBuilder;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class GameMapperTest {
    private final GameAnalysisMapper analysisMapper = new GameAnalysisMapper();
    private final GameMapper mapper = new GameMapper(analysisMapper);

    @Test
    void toDomain_mapsAllFields() {
        GameEntity entity = GameEntityMother.fullGameEntity();

        Game result = mapper.toDomain(entity);

        assertEquals(entity.getId(), result.getId());
        assertEquals(entity.getUserId(), result.getUserId());
        assertEquals(entity.getEvent(), result.getEvent());
        assertEquals(entity.getSite(), result.getSite());
        assertEquals(entity.getDate(), result.getDate());
        assertEquals(entity.getRound(), result.getRound());
        assertEquals(entity.getWhiteName(), result.getWhiteName());
        assertEquals(entity.getBlackName(), result.getBlackName());
        assertEquals(GameResult.WHITE_WIN, result.getResult());
        assertEquals(entity.getPgn(), result.getPgn());
        assertNotNull(result.getAnalysis());
        assertEquals(2, result.getMoves().size());
    }

    @Test
    void toDomain_returnsNull_whenEntityNull() {
        Game result = mapper.toDomain(null);

        assertNull(result);
    }

    @Test
    void toEntity_mapsAllFields() {
        GameMove move1 = GameMoveBuilder.move()
                .withPlyIndex(1)
                .withSan("e4")
                .withUci("e2e4")
                .withAnalysis(new GameMoveAnalysis(0.3, null, "e2e4", 0.1, MoveCategory.GOOD_MOVE))
                .build();
        GameMove move2 = GameMoveBuilder.move()
                .withPlyIndex(2)
                .withSan("e5")
                .withUci("e7e5")
                .build();
        Game domain = GameBuilder.game()
                .withId(UUID.fromString("99999999-9999-9999-9999-999999999999"))
                .withResult(GameResult.BLACK_WIN)
                .withMoves(Set.of(move1, move2))
                .build();

        GameEntity result = mapper.toEntity(domain);

        assertEquals(domain.getId(), result.getId());
        assertEquals(domain.getUserId(), result.getUserId());
        assertEquals(domain.getEvent(), result.getEvent());
        assertEquals(domain.getSite(), result.getSite());
        assertEquals(domain.getDate(), result.getDate());
        assertEquals(domain.getRound(), result.getRound());
        assertEquals(domain.getWhiteName(), result.getWhiteName());
        assertEquals(domain.getBlackName(), result.getBlackName());
        assertEquals(GameResult.BLACK_WIN.name(), result.getResult());
        assertEquals(domain.getPgn(), result.getPgn());
        assertEquals(2, result.getMoves().size());
    }

    @Test
    void toEntity_returnsNull_whenDomainNull() {
        GameEntity result = mapper.toEntity(null);

        assertNull(result);
    }

    @Test
    void movesToDomain_mapsMoveSet() {
        GameEntity entity = GameEntityMother.fullGameEntity();

        Set<GameMove> moves = mapper.movesToDomain(entity);

        assertEquals(2, moves.size());
        GameMove first = moves.iterator().next();
        assertNotNull(first.getSan());
    }

    @Test
    void movesToDomain_throws_whenEntityNull() {
        assertThrows(NullPointerException.class, () -> mapper.movesToDomain(null));
    }

    @Test
    void movesToEntity_mapsMoveSet() {
        GameMove move = GameMoveBuilder.move()
                .withPlyIndex(1)
                .withSan("e4")
                .withUci("e2e4")
                .build();
        Game domain = GameBuilder.game().withMoves(Set.of(move)).build();

        Set<GameMoveEntity> moves = mapper.movesToEntity(domain);

        assertEquals(1, moves.size());
        GameMoveEntity first = moves.iterator().next();
        assertEquals("e4", first.getSan());
    }

    @Test
    void movesToEntity_throws_whenDomainNull() {
        assertThrows(NullPointerException.class, () -> mapper.movesToEntity(null));
    }

    @Test
    void moveToDomain_mapsMove() {
        GameMoveEntity entity = GameMoveEntityBuilder.moveEntity()
                .withPlyIndex(7)
                .withSan("Nf3")
                .withUci("g1f3")
                .withAnalysisCategory("INACCURACY")
                .build();

        GameMove result = mapper.moveToDomain(entity);

        assertEquals(7, result.getPlyIndex());
        assertEquals("Nf3", result.getSan());
        assertEquals("g1f3", result.getUci());
        assertNotNull(result.getAnalysis());
        assertEquals(MoveCategory.INACCURACY, result.getAnalysis().category());
    }

    @Test
    void moveToDomain_returnsNull_whenEntityNull() {
        GameMove result = mapper.moveToDomain(null);

        assertNull(result);
    }

    @Test
    void moveToEntity_mapsMove() {
        GameMove move = GameMoveBuilder.move()
                .withPlyIndex(4)
                .withSan("Bb5")
                .withUci("f1b5")
                .withAnalysis(new GameMoveAnalysis(0.2, null, "f1b5", 0.05, MoveCategory.BEST_MOVE))
                .build();

        GameMoveEntity result = mapper.moveToEntity(move);

        assertEquals(4, result.getPlyIndex());
        assertEquals("Bb5", result.getSan());
        assertEquals("f1b5", result.getUci());
        assertEquals("BEST_MOVE", result.getAnalysisCategory());
    }

    @Test
    void moveToEntity_returnsNull_whenDomainNull() {
        GameMoveEntity result = mapper.moveToEntity(null);

        assertNull(result);
    }
}