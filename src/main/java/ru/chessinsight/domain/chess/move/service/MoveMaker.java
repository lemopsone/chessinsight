package ru.chessinsight.domain.chess.move.service;

import ru.chessinsight.domain.chess.move.model.Move;
import ru.chessinsight.domain.chess.move.model.MoveKind;
import ru.chessinsight.domain.chess.piece.model.*;
import ru.chessinsight.domain.chess.position.model.BoardCoordinates;
import ru.chessinsight.domain.chess.position.model.CastlingRights;
import ru.chessinsight.domain.chess.position.model.Chessboard;
import ru.chessinsight.domain.chess.position.model.Position;

public class MoveMaker {
    public static Position apply(Position p, Move m) {
        Color us = p.sideToMove();
        Chessboard b = p.board();
        CastlingRights cr = p.castlingRights();

        if (m.kind() == MoveKind.CASTLE_KING_SIDE || m.kind() == MoveKind.CASTLE_QUEEN_SIDE) {
            return applyCastling(p, m);
        }

        Piece moving = b.at(m.from());
        Chessboard newBoard = b.without(m.from());

        if (m.kind() == MoveKind.EN_PASSANT) {
            BoardCoordinates capturedPos =
                    new BoardCoordinates(m.to().rank() + (us == Color.WHITE ? -1 : 1), m.to().file());
            newBoard = newBoard.without(capturedPos);
        }

        Piece pieceToPlace = (m.kind() == MoveKind.PROMOTION)
                ? m.promotionTo().create(us)
                : moving;

        newBoard = newBoard.withPiece(m.to(), pieceToPlace);

        int halfmoves = (pieceToPlace instanceof Pawn || m.isCapture())
                ? 0
                : p.fiftyMoveRuleCounter() + 1;

        CastlingRights newRights = updateRightsAfterMove(cr, us, pieceToPlace, m.from(), m.to());

        BoardCoordinates enPassantSquare = (m.kind() == MoveKind.DOUBLE_PAWN_PUSH)
                ? new BoardCoordinates((m.from().rank() + m.to().rank()) / 2, m.to().file())
                : null;
        newBoard.setEnPassantSquare(enPassantSquare);

        int moveNum = p.turnNumber() + (us == Color.BLACK ? 1 : 0);

        return new Position(newBoard, us.opponent(), newRights, moveNum, halfmoves);
    }

    private static Position applyCastling(Position p, Move m) {
        Color c = p.sideToMove();
        CastlingRights cr = p.castlingRights();
        int kingFromFile = 4, rank = c == Color.WHITE ? 0 : 7;
        int kingToFile, rookFromFile, rookToFile;
        if (m.kind() == MoveKind.CASTLE_QUEEN_SIDE) {
            kingToFile = 2;
            rookFromFile = 0;
            rookToFile = 3;
        } else {
            kingToFile = 6;
            rookFromFile = 7;
            rookToFile = 5;
        }
        BoardCoordinates kingOldSquare = new BoardCoordinates(rank, kingFromFile),
                kingNewSquare = new BoardCoordinates(rank, kingToFile),
                rookOldSquare = new BoardCoordinates(rank, rookFromFile),
                rookNewSquare = new BoardCoordinates(rank, rookToFile);
        var newBoard = p.board().without(kingOldSquare).without(rookOldSquare)
                .withPiece(kingNewSquare, new King(c))
                .withPiece(rookNewSquare, new Rook(c));
        var newRights = c == Color.WHITE
                ? new CastlingRights(false, false, cr.blackQueenSide(), cr.blackKingSide())
                : new CastlingRights(cr.whiteQueenSide(), cr.whiteKingSide(), false, false);

        int turnNumber = p.turnNumber() + (c == Color.BLACK ? 1 : 0);

        return new Position(newBoard, c.opponent(), newRights, turnNumber, p.fiftyMoveRuleCounter() + 1);
    }

    private static CastlingRights updateRightsAfterMove(CastlingRights old,
                                                        Color c, Piece piece,
                                                        BoardCoordinates from, BoardCoordinates to) {
        boolean WK = old.whiteKingSide(), WQ = old.whiteQueenSide(), BK = old.blackKingSide(), BQ = old.blackQueenSide();
        if (piece instanceof King) {
            if (c == Color.WHITE) {
                WK = false; WQ = false;
            } else {
                BK = false; BQ = false;
            }
        } else if (piece instanceof Rook) {
            var startingRank = c == Color.WHITE ? 0 : 7;
            if (from.rank() == startingRank) {
                if (from.file() == 0) {
                    if (c == Color.WHITE) WQ = false; else BQ = false;
                } else if (from.file() == 7) {
                    if (c == Color.WHITE) WK = false; else BK = false;
                }
            }
        }
        if (to.rank() == 0 && to.file() == 0)
            WQ = false;
        else if (to.rank() == 0 && to.file() == 7)
            WK = false;
        else if (to.rank() == 7 && to.file() == 0)
            BQ = false;
        else if (to.rank() == 7 && to.file() == 7)
            BK = false;

        return new CastlingRights(WQ, WK, BQ, BK);
    }
}
