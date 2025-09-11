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
        int startingRank, promotionRank, dir;
        List<Move> moves = new ArrayList<>();
        if (color == Color.WHITE) {
            startingRank = 1;
            promotionRank = 6;
            dir = 1;
        } else {
            startingRank = 6;
            promotionRank = 1;
            dir = -1;
        }

        var forwardCoordinates = new BoardCoordinates(at.rank() + dir, at.file());
        if (b.at(forwardCoordinates) == null) {
            if (forwardCoordinates.rank() == promotionRank) {
                addPromotions(moves, at, forwardCoordinates, false);
            } else {
                moves.add(Move.move(at, forwardCoordinates, false));
            }
            if (at.rank() == startingRank) {
                var doublePushCoordinates = new BoardCoordinates(at.rank() + dir * 2, at.file());
                if (b.at(doublePushCoordinates) == null) {
                    moves.add(Move.doublePush(at, doublePushCoordinates));
                }
            }
        }

        for (int df : new int[]{-1, 1}) {
            var captureSquare = new BoardCoordinates(at.rank() + 1, at.file() + df);
            if (!b.inside(captureSquare)) continue;
            Piece target = b.at(captureSquare);
            if (target != null && target.color != this.color) {
                if (at.rank() == promotionRank) {
                    addPromotions(moves, at, captureSquare, true);
                } else {
                    moves.add(Move.move(at, captureSquare,true));
                }
            }
        }

        /* Взятие на проходе */
        BoardCoordinates enPassantSquare = b.getEnPassantSquare();
        if (enPassantSquare != null) {
            if (Math.abs(enPassantSquare.file() - at.file()) == 1
            && enPassantSquare.rank() == at.rank() + dir) {
                moves.add(Move.enPassant(at, enPassantSquare));
            }
        }

        return moves;
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
