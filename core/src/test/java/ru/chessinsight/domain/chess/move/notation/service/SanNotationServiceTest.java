package ru.chessinsight.domain.chess.move.notation.service;

import org.junit.jupiter.api.Test;
import ru.chessinsight.domain.chess.move.model.Move;
import ru.chessinsight.domain.chess.move.model.MoveKind;
import ru.chessinsight.domain.chess.move.service.MoveMaker;
import ru.chessinsight.domain.chess.position.model.Position;
import ru.chessinsight.domain.exception.DomainException;

import static org.junit.jupiter.api.Assertions.*;

public class SanNotationServiceTest {
    private final SanNotationService san = new SanNotationService();
    private final UciNotationService uci = new UciNotationService();

    @Test
    void san_move_pawn_and_knight() {
        Position p = Position.initial();
        Move e4 = san.sanToMove("e4", p);
        assertEquals("e2e4", uci.moveToUci(e4));
        Position p2 = MoveMaker.apply(p, e4);
        Move nc6 = san.sanToMove("Nc6", p2);
        assertEquals(MoveKind.NORMAL, nc6.kind());
        assertEquals("b8c6", uci.moveToUci(nc6));
        assertEquals("Nc6", san.moveToSan(nc6, p2));
    }

    @Test
    void castling_kingside_san() {
        Position p = Position.fromFEN("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        Move oo = san.sanToMove("O-O", p);
        assertEquals(MoveKind.CASTLE_KING_SIDE, oo.kind());
        assertEquals("O-O", san.moveToSan(oo, p));
    }

    @Test
    void castling_queenside_san() {
        Position p = Position.fromFEN("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        Move ooo = san.sanToMove("O-O-O", p);
        assertEquals(MoveKind.CASTLE_QUEEN_SIDE, ooo.kind());
        assertEquals("O-O-O", san.moveToSan(ooo, p));
    }

    @Test
    void promotion_white_san() {
        Position p = Position.fromFEN("4k3/P7/8/8/8/8/8/4K3 w K - 0 1");
        Move a8Q = san.sanToMove("a8=Q+", p);
        assertEquals("a7a8q", uci.moveToUci(a8Q));
        assertEquals("a8=Q+", san.moveToSan(a8Q, p));
    }

    @Test
    void promotion_black_san() {
        Position p = Position.fromFEN("4k3/8/8/8/8/8/p7/4K3 b K - 0 1");
        Move a1N = san.sanToMove("a1=B", p);
        assertEquals("a2a1b", uci.moveToUci(a1N));
        assertEquals("a1=B", san.moveToSan(a1N, p));
    }

    @Test
    void invalid_san_throws() {
        assertThrows(DomainException.class, () -> san.sanToMove("Qz9", Position.initial()));
        assertThrows(DomainException.class, () -> san.sanToMove("Zz9", Position.initial()));
        assertThrows(DomainException.class, () -> san.sanToMove("abc", Position.initial()));
    }

    @Test
    void san_resolves_pinned_knight() {
        Position pos = Position.initial();
        pos = MoveMaker.apply(pos, uci.uciToMove("d2d4", pos));
        pos = MoveMaker.apply(pos, uci.uciToMove("d7d5", pos));
        pos = MoveMaker.apply(pos, uci.uciToMove("b1c3", pos));
        pos = MoveMaker.apply(pos, uci.uciToMove("g8f6", pos));
        pos = MoveMaker.apply(pos, uci.uciToMove("c1g5", pos));
        pos = MoveMaker.apply(pos, uci.uciToMove("e7e6", pos));
        pos = MoveMaker.apply(pos, uci.uciToMove("e2e4", pos));
        pos = MoveMaker.apply(pos, uci.uciToMove("h7h6", pos));
        pos = MoveMaker.apply(pos, uci.uciToMove("g5f6", pos));
        pos = MoveMaker.apply(pos, uci.uciToMove("d8f6", pos));
        pos = MoveMaker.apply(pos, uci.uciToMove("e4d5", pos));
        pos = MoveMaker.apply(pos, uci.uciToMove("f8b4", pos));
        pos = MoveMaker.apply(pos, uci.uciToMove("d5e6", pos));
        pos = MoveMaker.apply(pos, uci.uciToMove("c8e6", pos));

        Move expected = uci.uciToMove("g1e2", pos);
        Move actual = san.sanToMove("Ne2", pos);
        assertEquals(expected, actual);
    }

    @Test
    void letterOf_returnsPieceLetter() {
        assertEquals("N", SanNotationService.letterOf("Knight"));
        assertEquals("Q", SanNotationService.letterOf("Queen"));
    }

    @Test
    void letterOf_returnsEmpty_forUnknownOrNull() {
        assertEquals("", SanNotationService.letterOf("Dragon"));
        assertEquals("", SanNotationService.letterOf(null));
    }
}