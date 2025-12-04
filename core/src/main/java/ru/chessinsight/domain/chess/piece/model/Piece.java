package ru.chessinsight.domain.chess.piece.model;

import ru.chessinsight.domain.chess.position.model.BoardCoordinates;
import ru.chessinsight.domain.chess.position.model.Chessboard;
import ru.chessinsight.domain.chess.move.model.Move;

import java.util.List;

public abstract class Piece {
    protected final Color color;

    public Piece(Color c) { this.color = c; }

    public Color getColor() {
        return color;
    }

    public abstract List<Move> pseudoLegalMoves(Chessboard b, BoardCoordinates at);
    public abstract char FENChar();


}
