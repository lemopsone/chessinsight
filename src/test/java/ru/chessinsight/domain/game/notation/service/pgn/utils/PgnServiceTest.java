package ru.chessinsight.domain.game.notation.service.pgn.utils;

import org.junit.jupiter.api.Test;
import ru.chessinsight.domain.chess.move.notation.service.SanNotationService;
import ru.chessinsight.domain.chess.move.notation.service.UciNotationService;
import ru.chessinsight.domain.game.notation.service.pgn.PgnService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
}
