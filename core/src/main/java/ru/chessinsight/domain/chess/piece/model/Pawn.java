package ru.chessinsight.domain.chess.piece.model;

import ru.chessinsight.domain.chess.position.model.BoardCoordinates;
import ru.chessinsight.domain.chess.position.model.Chessboard;
import ru.chessinsight.domain.chess.move.model.Move;

import java.util.ArrayList;
import java.util.List;

public class Pawn extends Piece {
    public Pawn(Color c) {
        super(c);
    }

    @Override
    public List<Move> pseudoLegalMoves(Chessboard b, BoardCoordinates at) {
        List<Move> moves = new ArrayList<>();
        int direction = color == Color.WHITE ? 1 : -1;
        int startingRank = color == Color.WHITE ? 1 : 6;
        int promotionRank = color == Color.WHITE ? 7 : 0;
        addForwardMoves(b, at, moves, direction, startingRank, promotionRank);
        addCaptureMoves(b, at, moves, direction, promotionRank);
        addEnPassantMove(b, at, moves, direction);
        return moves;
    }

    private void addForwardMoves(
            Chessboard board,
            BoardCoordinates at,
            List<Move> moves,
            int direction,
            int startingRank,
            int promotionRank
    ) {
        var forwardCoordinates = new BoardCoordinates(at.rank() + direction, at.file());
        if (board.at(forwardCoordinates) != null) {
            return;
        }
        if (forwardCoordinates.rank() == promotionRank) {
            addPromotions(moves, at, forwardCoordinates, false);
        } else {
            moves.add(Move.move(at, forwardCoordinates, false));
        }
        if (at.rank() == startingRank) {
            addDoublePushIfAvailable(board, at, moves, direction);
        }
    }

    private static void addDoublePushIfAvailable(Chessboard board, BoardCoordinates at, List<Move> moves, int direction) {
        var doublePushCoordinates = new BoardCoordinates(at.rank() + direction * 2, at.file());
        if (board.at(doublePushCoordinates) == null) {
            moves.add(Move.doublePush(at, doublePushCoordinates));
        }
    }

    private void addCaptureMoves(
            Chessboard board,
            BoardCoordinates at,
            List<Move> moves,
            int direction,
            int promotionRank
    ) {
        for (int fileDelta : new int[]{-1, 1}) {
            var captureSquare = new BoardCoordinates(at.rank() + direction, at.file() + fileDelta);
            if (!board.inside(captureSquare)) {
                continue;
            }
            Piece target = board.at(captureSquare);
            if (target == null || target.color == this.color) {
                continue;
            }
            if (at.rank() == promotionRank) {
                addPromotions(moves, at, captureSquare, true);
            } else {
                moves.add(Move.move(at, captureSquare, true));
            }
        }
    }

    private static void addEnPassantMove(Chessboard board, BoardCoordinates at, List<Move> moves, int direction) {
        BoardCoordinates enPassantSquare = board.getEnPassantSquare();
        if (enPassantSquare == null) {
            return;
        }
        if (Math.abs(enPassantSquare.file() - at.file()) == 1
                && enPassantSquare.rank() == at.rank() + direction) {
            moves.add(Move.enPassant(at, enPassantSquare));
        }
    }

    private void addPromotions(List<Move> moves, BoardCoordinates from, BoardCoordinates to, boolean isCapture) {
        moves.add(Move.promote(from, to, PromotionPieceType.QUEEN, isCapture));
        moves.add(Move.promote(from, to, PromotionPieceType.ROOK, isCapture));
        moves.add(Move.promote(from, to, PromotionPieceType.BISHOP, isCapture));
        moves.add(Move.promote(from, to, PromotionPieceType.KNIGHT, isCapture));
    }

    @Override
    public char FENChar() {
        return this.color == Color.WHITE ? 'P' : 'p';
    }
}
