package ru.chessinsight.domain.chess.move.service;

import ru.chessinsight.domain.chess.move.model.Move;
import ru.chessinsight.domain.chess.move.model.MoveKind;
import ru.chessinsight.domain.chess.piece.model.Color;
import ru.chessinsight.domain.chess.piece.model.King;
import ru.chessinsight.domain.chess.piece.model.Pawn;
import ru.chessinsight.domain.chess.piece.model.Piece;
import ru.chessinsight.domain.chess.piece.model.Rook;
import ru.chessinsight.domain.chess.position.model.BoardCoordinates;
import ru.chessinsight.domain.chess.position.model.CastlingRights;
import ru.chessinsight.domain.chess.position.model.Chessboard;
import ru.chessinsight.domain.chess.position.model.Position;

public class MoveMaker {
    public static Position apply(Position p, Move m) {
        Color us = p.sideToMove();
        Chessboard b = p.board();
        CastlingRights cr = p.castlingRights();

        if (isCastlingMove(m)) {
            return applyCastling(p, m);
        }

        Piece moving = b.at(m.from());
        Chessboard newBoard = b.without(m.from());
        newBoard = removeEnPassantCapture(newBoard, m, us);

        Piece pieceToPlace = resolvePieceToPlace(m, us, moving);

        newBoard = newBoard.withPiece(m.to(), pieceToPlace);

        int halfmoves = nextHalfMoveCounter(p, pieceToPlace, m);

        CastlingRights newRights = updateRightsAfterMove(cr, us, pieceToPlace, m.from(), m.to());

        newBoard.setEnPassantSquare(resolveEnPassantSquare(m));

        int moveNum = p.turnNumber() + (us == Color.BLACK ? 1 : 0);

        return new Position(newBoard, us.opponent(), newRights, moveNum, halfmoves);
    }

    private static boolean isCastlingMove(Move move) {
        return move.kind() == MoveKind.CASTLE_KING_SIDE || move.kind() == MoveKind.CASTLE_QUEEN_SIDE;
    }

    private static Chessboard removeEnPassantCapture(Chessboard board, Move move, Color sideToMove) {
        if (move.kind() != MoveKind.EN_PASSANT) {
            return board;
        }
        int capturedPawnRankOffset = sideToMove == Color.WHITE ? -1 : 1;
        BoardCoordinates capturedPos = new BoardCoordinates(
                move.to().rank() + capturedPawnRankOffset,
                move.to().file()
        );
        return board.without(capturedPos);
    }

    private static Piece resolvePieceToPlace(Move move, Color sideToMove, Piece movingPiece) {
        if (move.kind() == MoveKind.PROMOTION) {
            return move.promotionTo().create(sideToMove);
        }
        return movingPiece;
    }

    private static int nextHalfMoveCounter(Position position, Piece movedPiece, Move move) {
        if (movedPiece instanceof Pawn || move.isCapture()) {
            return 0;
        }
        return position.fiftyMoveRuleCounter() + 1;
    }

    private static BoardCoordinates resolveEnPassantSquare(Move move) {
        if (move.kind() != MoveKind.DOUBLE_PAWN_PUSH) {
            return null;
        }
        return new BoardCoordinates((move.from().rank() + move.to().rank()) / 2, move.to().file());
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
        RightsState rights = RightsState.from(old);
        revokeForMovingPiece(rights, c, piece, from);
        revokeForCapturedCornerRook(rights, to);
        return rights.toCastlingRights();
    }

    private static void revokeForMovingPiece(RightsState rights, Color color, Piece piece, BoardCoordinates from) {
        if (piece instanceof King) {
            rights.revokeBoth(color);
            return;
        }
        if (piece instanceof Rook && isStartingRookSquare(color, from)) {
            rights.revokeRookSide(color, from.file());
        }
    }

    private static boolean isStartingRookSquare(Color color, BoardCoordinates from) {
        int startingRank = color == Color.WHITE ? 0 : 7;
        return from.rank() == startingRank && (from.file() == 0 || from.file() == 7);
    }

    private static void revokeForCapturedCornerRook(RightsState rights, BoardCoordinates to) {
        int square = (to.rank() * 8) + to.file();
        switch (square) {
            case 0 -> rights.whiteQueenSide = false;
            case 7 -> rights.whiteKingSide = false;
            case 56 -> rights.blackQueenSide = false;
            case 63 -> rights.blackKingSide = false;
            default -> {
            }
        }
    }

    private static final class RightsState {
        private boolean whiteKingSide;
        private boolean whiteQueenSide;
        private boolean blackKingSide;
        private boolean blackQueenSide;

        private static RightsState from(CastlingRights rights) {
            RightsState state = new RightsState();
            state.whiteKingSide = rights.whiteKingSide();
            state.whiteQueenSide = rights.whiteQueenSide();
            state.blackKingSide = rights.blackKingSide();
            state.blackQueenSide = rights.blackQueenSide();
            return state;
        }

        private void revokeBoth(Color color) {
            if (color == Color.WHITE) {
                whiteKingSide = false;
                whiteQueenSide = false;
                return;
            }
            blackKingSide = false;
            blackQueenSide = false;
        }

        private void revokeRookSide(Color color, int file) {
            if (color == Color.WHITE) {
                if (file == 0) {
                    whiteQueenSide = false;
                } else {
                    whiteKingSide = false;
                }
                return;
            }
            if (file == 0) {
                blackQueenSide = false;
            } else {
                blackKingSide = false;
            }
        }

        private CastlingRights toCastlingRights() {
            return new CastlingRights(whiteQueenSide, whiteKingSide, blackQueenSide, blackKingSide);
        }
    }
}
