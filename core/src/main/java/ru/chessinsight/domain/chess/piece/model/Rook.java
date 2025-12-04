package ru.chessinsight.domain.chess.piece.model;

import ru.chessinsight.domain.chess.position.model.BoardCoordinates;
import ru.chessinsight.domain.chess.position.model.Chessboard;
import ru.chessinsight.domain.chess.move.model.Move;
import ru.chessinsight.domain.chess.move.service.MoveGenerator;

import java.util.List;

public class Rook extends Piece {

    public Rook(Color c) {
        super(c);
    }

    @Override
    public List<Move> pseudoLegalMoves(Chessboard b, BoardCoordinates at) {
        return MoveGenerator.orthogonal(b, at, color);
    }

    @Override
    public char FENChar() {
        return this.color == Color.WHITE ? 'R' : 'r';
    }
}
