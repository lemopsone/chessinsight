package ru.chessinsight.domain.chess.piece.model;

import ru.chessinsight.domain.chess.position.model.BoardCoordinates;
import ru.chessinsight.domain.chess.position.model.Chessboard;
import ru.chessinsight.domain.chess.move.model.Move;
import ru.chessinsight.domain.chess.move.service.MoveGenerator;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Queen extends Piece {
    public Queen(Color c) {
        super(c);
    }

    @Override
    public List<Move> pseudoLegalMoves(Chessboard b, BoardCoordinates at) {
        return Stream.of(
                MoveGenerator.diagonal(b, at, color),
                MoveGenerator.orthogonal(b, at, color)
        ).flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public char FENChar() {
        return this.color == Color.WHITE ? 'Q' : 'q';
    }
}
