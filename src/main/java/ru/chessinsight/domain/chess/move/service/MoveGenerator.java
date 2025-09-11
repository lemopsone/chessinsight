package ru.chessinsight.domain.chess.move.service;

import ru.chessinsight.domain.chess.position.model.BoardCoordinates;
import ru.chessinsight.domain.chess.position.model.Chessboard;
import ru.chessinsight.domain.chess.move.model.MoveOffset;
import ru.chessinsight.domain.chess.piece.model.Color;
import ru.chessinsight.domain.chess.move.model.Move;
import ru.chessinsight.domain.chess.piece.model.Piece;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class MoveGenerator {
    private static final class TraceDirection {
        public static short UP = 2;
        public static short DOWN = 0;
        public static short LEFT = 6;
        public static short RIGHT = 4;
    }

    public static List<Move> orthogonal(Chessboard b, BoardCoordinates from, Color color) {
        return Stream.of(
                inDirection(b, from, color, TraceDirection.UP),
                inDirection(b, from, color, TraceDirection.DOWN),
                inDirection(b, from, color, TraceDirection.LEFT),
                inDirection(b, from, color, TraceDirection.RIGHT)
        ).flatMap(Collection::stream).toList();
    }

    public static List<Move> diagonal(Chessboard b, BoardCoordinates from, Color color) {
        return Stream.of(
                inDirection(b, from, color, TraceDirection.UP | TraceDirection.LEFT),
                inDirection(b, from, color, TraceDirection.UP | TraceDirection.RIGHT),
                inDirection(b, from, color, TraceDirection.DOWN | TraceDirection.LEFT),
                inDirection(b, from, color, TraceDirection.DOWN | TraceDirection.RIGHT)
        ).flatMap(Collection::stream).toList();
    }

    private static List<Move> inDirection(Chessboard b, BoardCoordinates from, Color color, int direction) {
        int verticalOffset = (direction % 4) - 1;
        int horizontalOffset = (direction >> 2 % 4) - 1;
        var moveList = new ArrayList<Move>();
        BoardCoordinates next =
                new BoardCoordinates(from.rank() + verticalOffset, from.file() + horizontalOffset);
        while (b.inside(next)) {
            Move nextPossibleMove = generateMoveIfPossible(b, from, next, color);
            if (nextPossibleMove == null) break;
            else moveList.add(nextPossibleMove);
            next = new BoardCoordinates(next.rank() + verticalOffset, next.file() + horizontalOffset);
        }

        return moveList;
    }

    public static List<Move> byOffsetList(Chessboard b, BoardCoordinates from, Color color, List<MoveOffset> offsets) {
        var moveList = new ArrayList<Move>();
        for (MoveOffset offset : offsets) {
            var move = generateMoveIfPossible(b, from,
                    new BoardCoordinates(from.rank() + offset.dy(), from.file() + offset.dx()),
                    color);
            if (move != null) { moveList.add(move); }
        }
        return moveList;
    }

    public static Move generateMoveIfPossible(Chessboard b, BoardCoordinates src, BoardCoordinates target, Color color) {
        Piece occupant = b.at(target);
        if (occupant == null) {
            return Move.move(src, target, false);
        } else if (occupant.getColor() != color) {
            return Move.move(src, target, true);
        }
        return null;
    }
}
