package ru.chessinsight.domain.chess.move.service;

import org.junit.jupiter.api.Test;
import ru.chessinsight.domain.chess.move.model.Move;
import ru.chessinsight.domain.chess.position.model.BoardCoordinates;
import ru.chessinsight.domain.chess.position.model.Position;

import static org.junit.jupiter.api.Assertions.*;

public class MoveMakerTest {

    @Test
    void apply_simple_move_updates_fen() {
        Position p = Position.initial();
        Move m = Move.move(BoardCoordinates.fromString("e2"), BoardCoordinates.fromString("e4"), false);
        Position after = MoveMaker.apply(p, m);
        assertEquals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1", after.toFEN());
    }
}
