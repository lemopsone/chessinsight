package ru.chessinsight.domain.chess.move.service;

import org.junit.jupiter.api.Test;
import ru.chessinsight.domain.chess.move.model.Move;
import ru.chessinsight.domain.chess.move.model.MoveKind;
import ru.chessinsight.domain.chess.position.model.Position;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoveValidatorTest {
    private final MoveValidator validator = new MoveValidator();
    private final ru.chessinsight.domain.chess.move.notation.service.UciNotationService uci =
            new ru.chessinsight.domain.chess.move.notation.service.UciNotationService();

    @Test
    void generate_legal_moves_contains_known_opening_moves() {
        Position p = Position.initial();
        List<Move> legal = validator.sideLegalMoves(p, p.sideToMove());
        boolean hasE4 = legal.stream().anyMatch(m -> "e2e4".equals(uci.moveToUci(m)));
        boolean hasNf3 = legal.stream().anyMatch(m -> "g1f3".equals(uci.moveToUci(m)));
        assertTrue(hasE4);
        assertTrue(hasNf3);
    }

    @Test
    void castling_legal_when_path_clear() {
        Position p = Position.fromFEN("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        List<Move> legal = validator.sideLegalMoves(p, p.sideToMove());
        boolean hasOO = legal.stream().anyMatch(m -> m.kind()== MoveKind.CASTLE_KING_SIDE);
        boolean hasOOO = legal.stream().anyMatch(m -> m.kind()== MoveKind.CASTLE_QUEEN_SIDE);
        assertTrue(hasOO);
        assertTrue(hasOOO);
    }
}
