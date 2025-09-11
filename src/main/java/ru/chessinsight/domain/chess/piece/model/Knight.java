package ru.chessinsight.domain.chess.piece.model;

import ru.chessinsight.domain.chess.position.model.BoardCoordinates;
import ru.chessinsight.domain.chess.position.model.Chessboard;
import ru.chessinsight.domain.chess.move.model.Move;
import ru.chessinsight.domain.chess.move.service.MoveGenerator;
import ru.chessinsight.domain.chess.move.model.MoveOffset;

import java.util.Arrays;
import java.util.List;

public class Knight extends Piece {
    private static final int[][] offsets = {
            {-2, 1}, {2, 1},
            {-2, -1}, {2, -1},
            {1, -2}, {-1, -2},
            {1, 2}, {-1, -2}
    };

    public Knight(Color c) {
        super(c);
    }

    @Override
    public List<Move> pseudoLegalMoves(Chessboard b, BoardCoordinates at) {
        var offsets = Arrays.stream(Knight.offsets).map(val -> new MoveOffset(val[0], val[1])).toList();
        return MoveGenerator.byOffsetList(b, at, color, offsets);
    }

    @Override
    public char FENChar() {
        return this.color == Color.WHITE ? 'N' : 'n';
    }
}
