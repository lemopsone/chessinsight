package ru.chessinsight.domain.chess.position.model;

import org.junit.jupiter.api.Test;
import ru.chessinsight.domain.chess.piece.model.Color;
import static org.junit.jupiter.api.Assertions.*;

public class PositionFenTest {

    @Test
    void initial_position_roundtrip_fen() {
        Position p = Position.initial();
        String fen = p.toFEN();
        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", fen);
        Position p2 = Position.fromFEN(fen);
        assertEquals(p2.toFEN(), fen);
        assertEquals(Color.WHITE, p2.sideToMove());
    }

    @Test
    void invalid_fen_throws() {
        assertThrows(RuntimeException.class, () -> Position.fromFEN("bad fen"));
        assertThrows(RuntimeException.class, () -> Position.fromFEN("8/8/8/8/8/8/8/8 w - - 0"));
    }
}
