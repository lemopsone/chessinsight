package ru.chessinsight.domain.chess.move.service;

import org.junit.jupiter.api.Test;
import ru.chessinsight.domain.chess.move.model.Move;
import ru.chessinsight.domain.chess.position.model.BoardCoordinates;
import ru.chessinsight.domain.chess.position.model.Position;
import io.qameta.allure.Tag;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class MoveMakerTest {

    @Test
    void apply_simple_move_updates_fen() {
        Position p = Position.initial();
        Move m = Move.move(BoardCoordinates.fromString("e2"), BoardCoordinates.fromString("e4"), false);
        Position after = MoveMaker.apply(p, m);
        assertEquals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1", after.toFEN());
    }

    @Test
    void apply_castling_updates_king_and_rook() {
        Position p = Position.fromFEN("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        Move castle = Move.castleKingSide(p.sideToMove());
        Position after = MoveMaker.apply(p, castle);
        assertEquals("r3k2r/8/8/8/8/8/8/R4RK1 b kq - 1 1", after.toFEN());
    }

    @Test
    void apply_throws_whenMoveNull() {
        Position p = Position.initial();
        assertThrows(NullPointerException.class, () -> MoveMaker.apply(p, null));
    }
}