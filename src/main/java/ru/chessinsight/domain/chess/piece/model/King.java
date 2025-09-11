package ru.chessinsight.domain.chess.piece.model;

import ru.chessinsight.domain.chess.position.model.BoardCoordinates;
import ru.chessinsight.domain.chess.position.model.Chessboard;
import ru.chessinsight.domain.chess.move.model.Move;
import ru.chessinsight.domain.chess.move.service.MoveGenerator;

import java.util.ArrayList;
import java.util.List;

public class King extends Piece {
    public King(Color c) { super(c); }

    @Override
    public List<Move> pseudoLegalMoves(Chessboard b, BoardCoordinates at) {
        var moves = new ArrayList<Move>();
        for (int dx = -1; dx < 2; dx++) {
            for (int dy = -1; dy < 2; dy++) {
                if (dx == 0 && dy == 0) continue;
                var move = MoveGenerator.generateMoveIfPossible(b, at,
                        new BoardCoordinates(at.rank() + dx, at.file() + dy),
                        color);
                if (move != null) moves.add(move);
            }
        }
        return moves;
    }

    @Override
    public char FENChar() {
        return this.color == Color.WHITE ? 'K' : 'k';
    }
}
