package ru.chessinsight.domain.chess.move.service;

import org.junit.jupiter.api.Test;
import ru.chessinsight.domain.chess.move.model.Move;
import ru.chessinsight.domain.chess.move.model.MoveOffset;
import ru.chessinsight.domain.chess.piece.model.Color;
import ru.chessinsight.domain.chess.piece.model.Pawn;
import ru.chessinsight.domain.chess.position.model.BoardCoordinates;
import ru.chessinsight.domain.chess.position.model.Chessboard;

import java.util.List;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class MoveGeneratorTest {

    @Test
    void orthogonal_stopsAtCapture() {
        Chessboard board = Chessboard.empty()
                .withPiece(BoardCoordinates.fromString("d6"), new Pawn(Color.BLACK));
        BoardCoordinates from = BoardCoordinates.fromString("d4");

        List<Move> moves = MoveGenerator.orthogonal(board, from, Color.WHITE);

        assertTrue(moves.stream().anyMatch(m -> m.to().equals(BoardCoordinates.fromString("d6")) && m.isCapture()));
        assertFalse(moves.stream().anyMatch(m -> m.to().equals(BoardCoordinates.fromString("d7"))));
    }

    @Test
    void orthogonal_blocksOnFriendlyPiece() {
        Chessboard board = Chessboard.empty()
                .withPiece(BoardCoordinates.fromString("d5"), new Pawn(Color.WHITE));
        BoardCoordinates from = BoardCoordinates.fromString("d4");

        List<Move> moves = MoveGenerator.orthogonal(board, from, Color.WHITE);

        assertFalse(moves.stream().anyMatch(m -> m.to().equals(BoardCoordinates.fromString("d5"))));
    }

    @Test
    void diagonal_stopsAtCapture() {
        Chessboard board = Chessboard.empty()
                .withPiece(BoardCoordinates.fromString("g7"), new Pawn(Color.BLACK));
        BoardCoordinates from = BoardCoordinates.fromString("d4");

        List<Move> moves = MoveGenerator.diagonal(board, from, Color.WHITE);

        assertTrue(moves.stream().anyMatch(m -> m.to().equals(BoardCoordinates.fromString("g7")) && m.isCapture()));
        assertFalse(moves.stream().anyMatch(m -> m.to().equals(BoardCoordinates.fromString("h8"))));
    }

    @Test
    void diagonal_blocksOnFriendlyPiece() {
        Chessboard board = Chessboard.empty()
                .withPiece(BoardCoordinates.fromString("e5"), new Pawn(Color.WHITE));
        BoardCoordinates from = BoardCoordinates.fromString("d4");

        List<Move> moves = MoveGenerator.diagonal(board, from, Color.WHITE);

        assertFalse(moves.stream().anyMatch(m -> m.to().equals(BoardCoordinates.fromString("e5"))));
    }

    @Test
    void byOffsetList_generatesMovesInsideBoard() {
        Chessboard board = Chessboard.empty();
        BoardCoordinates from = BoardCoordinates.fromString("b1");
        List<MoveOffset> offsets = List.of(new MoveOffset(1, 2), new MoveOffset(2, 1));

        List<Move> moves = MoveGenerator.byOffsetList(board, from, Color.WHITE, offsets);

        assertEquals(2, moves.size());
        assertTrue(moves.stream().anyMatch(m -> m.to().equals(BoardCoordinates.fromString("c3"))));
        assertTrue(moves.stream().anyMatch(m -> m.to().equals(BoardCoordinates.fromString("d2"))));
    }

    @Test
    void byOffsetList_skipsOutsideMoves() {
        Chessboard board = Chessboard.empty();
        BoardCoordinates from = BoardCoordinates.fromString("a1");
        List<MoveOffset> offsets = List.of(new MoveOffset(-1, -2), new MoveOffset(2, 1));

        List<Move> moves = MoveGenerator.byOffsetList(board, from, Color.WHITE, offsets);

        assertEquals(1, moves.size());
        assertTrue(moves.stream().anyMatch(m -> m.to().equals(BoardCoordinates.fromString("c2"))));
    }

    @Test
    void generateMoveIfPossible_returnsCaptureForEnemy() {
        Chessboard board = Chessboard.empty()
                .withPiece(BoardCoordinates.fromString("c3"), new Pawn(Color.BLACK));
        Move move = MoveGenerator.generateMoveIfPossible(board, BoardCoordinates.fromString("b2"), BoardCoordinates.fromString("c3"), Color.WHITE);

        assertNotNull(move);
        assertTrue(move.isCapture());
    }

    @Test
    void generateMoveIfPossible_returnsNullForFriendly() {
        Chessboard board = Chessboard.empty()
                .withPiece(BoardCoordinates.fromString("c3"), new Pawn(Color.WHITE));
        Move move = MoveGenerator.generateMoveIfPossible(board, BoardCoordinates.fromString("b2"), BoardCoordinates.fromString("c3"), Color.WHITE);

        assertNull(move);
    }
}