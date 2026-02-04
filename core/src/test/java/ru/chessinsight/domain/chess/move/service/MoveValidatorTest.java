package ru.chessinsight.domain.chess.move.service;

import org.junit.jupiter.api.Test;
import ru.chessinsight.domain.chess.move.model.Move;
import ru.chessinsight.domain.chess.move.model.MoveKind;
import ru.chessinsight.domain.chess.position.model.BoardCoordinates;
import ru.chessinsight.domain.chess.position.model.Position;

import java.util.List;
import io.qameta.allure.Tag;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
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

    @Test
    void isLegal_returnsTrue_forLegalMove() {
        Position p = Position.initial();
        Move m = uci.uciToMove("e2e4", p);

        boolean result = validator.isLegal(p, m);

        assertTrue(result);
    }

    @Test
    void isLegal_returnsFalse_forIllegalMove() {
        Position p = Position.initial();
        Move m = Move.move(BoardCoordinates.fromString("e2"), BoardCoordinates.fromString("e5"), false);

        boolean result = validator.isLegal(p, m);

        assertFalse(result);
    }

    @Test
    void isKingInCheck_returnsTrue_whenInCheck() {
        Position p = Position.fromFEN("4k3/8/8/8/8/8/4R3/4K3 b - - 0 1");

        boolean result = validator.isKingInCheck(p, p.sideToMove());

        assertTrue(result);
    }

    @Test
    void isKingInCheck_returnsFalse_whenSafe() {
        Position p = Position.initial();

        boolean result = validator.isKingInCheck(p, p.sideToMove());

        assertFalse(result);
    }

    @Test
    void pieceLegalMoves_returnsMoves_forPawn() {
        Position p = Position.initial();

        List<Move> moves = validator.pieceLegalMoves(p, BoardCoordinates.fromString("e2"));

        assertFalse(moves.isEmpty());
        assertTrue(moves.stream().anyMatch(m -> m.to().equals(BoardCoordinates.fromString("e3"))));
    }

    @Test
    void pieceLegalMoves_returnsEmpty_whenNoPiece() {
        Position p = Position.initial();

        List<Move> moves = validator.pieceLegalMoves(p, BoardCoordinates.fromString("e5"));

        assertTrue(moves.isEmpty());
    }
}