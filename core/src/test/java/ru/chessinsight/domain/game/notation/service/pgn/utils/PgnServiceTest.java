package ru.chessinsight.domain.game.notation.service.pgn.utils;

import org.junit.jupiter.api.Test;
import ru.chessinsight.domain.chess.move.notation.service.SanNotationService;
import ru.chessinsight.domain.chess.move.notation.service.UciNotationService;
import ru.chessinsight.domain.exception.BadBoardCoordinatesException;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.model.GameMove;
import ru.chessinsight.domain.game.notation.service.pgn.PgnService;
import ru.chessinsight.testdata.GameMoveBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
public class PgnServiceTest {
    final PgnService pgnService = new PgnService();
    final SanNotationService sanNotationService = new SanNotationService();
    final UciNotationService uciNotationService = new UciNotationService();
    static String pgn = """
            [Event "Friendly"]
            1. e4 e5 (1... c5 2. Nf3) 2. Nf3 *
            """;

    @Test
    void correctly_parses_from_pgn() {
        var game = pgnService.createEmptyGameFromPGN(pgn);
        assertEquals("Friendly", game.getEvent());
        var moves = pgnService.extractMovesFromGameMainline(game);
        assertEquals("e4", moves.getFirst().getSan());
        assertEquals(1, moves.getFirst().getPlyIndex());
        assertEquals("e5", moves.get(1).getSan());
        assertEquals(2, moves.get(1).getPlyIndex());
        assertEquals("Nf3", moves.get(2).getSan());
        assertEquals(3,moves.get(2).getPlyIndex());
    }

    @Test
    void parse_returns_ast() {
        var ast = pgnService.parse(pgn);

        assertNotNull(ast);
        assertEquals("Friendly", ast.tags.get("Event"));
    }

    @Test
    void parse_handles_empty_pgn() {
        var ast = pgnService.parse("");

        assertNotNull(ast);
        assertEquals("*", ast.result);
    }

    @Test
    void toPgn_includesMoves() {
        Game game = new Game();
        List<GameMove> moves = List.of(
                GameMoveBuilder.move().withPlyIndex(1).withSan("e4").build(),
                GameMoveBuilder.move().withPlyIndex(2).withSan("e5").build()
        );

        String out = pgnService.toPgn(game, moves);

        assertTrue(out.contains("1. e4 e5"));
    }

    @Test
    void toPgn_endsWithResultStar_whenNoMoves() {
        Game game = new Game();
        String out = pgnService.toPgn(game, List.of());

        assertTrue(out.trim().endsWith("*"));
    }

    @Test
    void createEmptyGameFromPGN_setsNullDate_whenMissing() {
        String text = """
            [Event "NoDate"]
            1. e4 e5 *
            """;
        var game = pgnService.createEmptyGameFromPGN(text);

        assertNull(game.getDate());
        assertEquals("NoDate", game.getEvent());
    }

    @Test
    void extractMoves_throws_onInvalidSan() {
        String bad = """
            [Event "Bad"]
            1. Qz9 *
            """;
        Game game = pgnService.createEmptyGameFromPGN(bad);

        assertThrows(BadBoardCoordinatesException.class, () -> pgnService.extractMovesFromGameMainline(game));
    }
}