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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MoveGenerator {
    private static final class TraceDirection {
        public static final short UP = 1;
        public static final short DOWN = 2;
        public static final short LEFT = 4;
        public static final short RIGHT = 8;
    }

    public static List<Move> orthogonal(Chessboard b, BoardCoordinates from, Color color) {
        return Stream.of(
                inDirection(b, from, color, TraceDirection.UP),
                inDirection(b, from, color, TraceDirection.DOWN),
                inDirection(b, from, color, TraceDirection.LEFT),
                inDirection(b, from, color, TraceDirection.RIGHT)
        ).flatMap(Collection::stream).collect(Collectors.toCollection(ArrayList::new));
    }

    public static List<Move> diagonal(Chessboard b, BoardCoordinates from, Color color) {
        return Stream.of(
                inDirection(b, from, color, TraceDirection.UP | TraceDirection.LEFT),
                inDirection(b, from, color, TraceDirection.UP | TraceDirection.RIGHT),
                inDirection(b, from, color, TraceDirection.DOWN | TraceDirection.LEFT),
                inDirection(b, from, color, TraceDirection.DOWN | TraceDirection.RIGHT)
        ).flatMap(Collection::stream).collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<Move> inDirection(Chessboard b, BoardCoordinates from, Color color, int direction) {
        int verticalOffset =
                ((direction & TraceDirection.UP)    != 0 ? 1 : 0) +
                ((direction & TraceDirection.DOWN)  != 0 ? -1 : 0);
        int horizontalOffset =
                ((direction & TraceDirection.RIGHT) != 0 ? 1 : 0) +
                ((direction & TraceDirection.LEFT)  != 0 ? -1 : 0);
        var moveList = new ArrayList<Move>();
        BoardCoordinates next =
                new BoardCoordinates(from.rank() + verticalOffset, from.file() + horizontalOffset);
        while (b.inside(next)) {
            Move nextPossibleMove = generateMoveIfPossible(b, from, next, color);
            if (nextPossibleMove == null) break;
            else moveList.add(nextPossibleMove);
            if (nextPossibleMove.isCapture()) break;
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
        if (!b.inside(target)) return null;
        Piece occupant = b.at(target);
        if (occupant == null) {
            return Move.move(src, target, false);
        } else if (occupant.getColor() != color) {
            return Move.move(src, target, true);
        }
        return null;
    }
}
