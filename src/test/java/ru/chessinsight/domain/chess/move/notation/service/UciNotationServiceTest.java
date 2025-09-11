package ru.chessinsight.domain.chess.move.notation.service;

import org.junit.jupiter.api.Test;
import ru.chessinsight.domain.chess.move.model.Move;
import ru.chessinsight.domain.chess.move.model.MoveKind;
import ru.chessinsight.domain.chess.position.model.Position;
import ru.chessinsight.domain.exception.DomainException;

import static org.junit.jupiter.api.Assertions.*;

public class UciNotationServiceTest {
    private final UciNotationService uci = new UciNotationService();

    @Test
    void moveToUci_and_back() {
        Position p = Position.initial();
        Move m = uci.uciToMove("e2e4", p);
        assertEquals("e2e4", uci.moveToUci(m));
    }

    @Test
    void castling_kingside_san() {
        Position p = Position.fromFEN("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        Move oo = uci.uciToMove("e1g1", p);
        assertEquals(MoveKind.CASTLE_KING_SIDE, oo.kind());
    }

    @Test
    void castling_queenside_san() {
        Position p = Position.fromFEN("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        Move ooo = uci.uciToMove("e1c1", p);
        assertEquals(MoveKind.CASTLE_QUEEN_SIDE, ooo.kind());
    }

    @Test
    void promotion_white_san() {
        Position p = Position.fromFEN("4k3/P7/8/8/8/8/8/4K3 w K - 0 1");
        Move a8Q = uci.uciToMove("a7a8q", p);
        assertEquals("a7a8q", uci.moveToUci(a8Q));
    }

    @Test
    void promotion_black_san() {
        Position p = Position.fromFEN("4k3/8/8/8/8/8/p7/4K3 b K - 0 1");
        Move a1Q = uci.uciToMove("a2a1q", p);
        assertEquals("a2a1q", uci.moveToUci(a1Q));
    }

    @Test
    void invalid_uci_throws() {
        assertThrows(DomainException.class, () -> uci.uciToMove("e9e4", Position.initial()));
        assertThrows(DomainException.class, () -> uci.uciToMove("e2e", Position.initial()));
    }

}
