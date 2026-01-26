package ru.chessinsight.domain.chess.move.service;

import ru.chessinsight.domain.chess.position.model.BoardCoordinates;
import ru.chessinsight.domain.chess.position.model.Chessboard;
import ru.chessinsight.domain.chess.position.model.Position;
import ru.chessinsight.domain.chess.move.model.Move;
import ru.chessinsight.domain.chess.move.model.MoveKind;
import ru.chessinsight.domain.chess.piece.model.Color;
import ru.chessinsight.domain.chess.piece.model.King;
import ru.chessinsight.domain.chess.piece.model.Piece;
import ru.chessinsight.domain.chess.piece.model.Rook;

import java.util.*;

public class MoveValidator {
    private static final int KING_FILE = 4;

    public boolean isLegal(Position p, Move m) {
        if (m.kind() == MoveKind.CASTLE_KING_SIDE || m.kind() == MoveKind.CASTLE_QUEEN_SIDE) {
            return isCastlingLegal(p, m);
        }
        if (!containedInPieceMoves(p, m)) return false;
        Position after = MoveMaker.apply(p, m);
        return !isKingInCheck(after, p.sideToMove());
    }

    public boolean isKingInCheck(Position p, Color c) {
        var kingCoords = p.board().getKingSquare(c);
        var attackedSquares = getSideAttackedSquares(p, c.opponent());
        return attackedSquares.contains(kingCoords);
    }

    private boolean containedInPieceMoves(Position p, Move m) {
        var startingPosition = m.from();
        Chessboard b = p.board();
        Piece piece = b.at(startingPosition);
        if (piece == null) return false;
        return piece.pseudoLegalMoves(b, startingPosition).contains(m);
    }


    private boolean isCastlingLegal(Position p, Move m) {
        Color stm = p.sideToMove();
        int rank = getKingRank(stm);
        int rookFile = getRookFile(m.kind());
        if (!castlingPathEmpty(p, rank, rookFile)) { return false; }
        if (!rookPresent(p, rank, rookFile, stm)) { return false; }
        return !castlingPathIsAttacked(p, stm, m.kind());
    }

    private boolean castlingPathIsAttacked(Position p, Color c, MoveKind kind) {
        int rank = getKingRank(c);
        int finalKingPos = getFinalCastlingPosition(kind);

        List<BoardCoordinates> squaresToCheck = new ArrayList<>();
        squaresToCheck.add(new BoardCoordinates(rank, KING_FILE));
        squaresToCheck.add(new BoardCoordinates(rank, finalKingPos));
        squaresToCheck.add(new BoardCoordinates(rank, (KING_FILE + finalKingPos) / 2));

        Set<BoardCoordinates> attackedSquares = getSideAttackedSquares(p, c.opponent());
        for (var square : squaresToCheck) {
            if (attackedSquares.contains(square)) return true;
        }
        return false;
    }

    private Set<BoardCoordinates> getSideAttackedSquares(Position p, Color c) {
        var enemyPieces = p.board().getPiecesByColor(c);
        for (Map.Entry<BoardCoordinates, Piece> piece : enemyPieces.entrySet()) {
            piece.getValue().pseudoLegalMoves(p.board(), piece.getKey());
        }
        Set<BoardCoordinates> squares = new HashSet<>();
        enemyPieces.forEach((key, value) -> value.pseudoLegalMoves(p.board(), key).stream().map(Move::to)
             .forEach(squares::add));

        return squares;
    }

    private boolean castlingPathEmpty(Position p, int rank, int rookFile) {
        int startFile = Math.min(rookFile, KING_FILE);
        int endFile = Math.max(rookFile, KING_FILE);
        for (int i = startFile + 1; i < endFile; ++i) {
            if (p.board().at(new BoardCoordinates(rank, i)) != null) {
                return false;
            }
        }
        return true;
    }

    private boolean rookPresent(Position p, int rank, int rookFile, Color c) {
        BoardCoordinates rookPosition = new BoardCoordinates(rank, rookFile);
        Piece existingPiece = p.board().at(rookPosition);
        return (existingPiece instanceof Rook && existingPiece.getColor() == c);
    }

    public List<Move> pieceLegalMoves(Position p, BoardCoordinates coords) {
        Piece piece = p.board().at(coords);
        if (piece == null) return List.of();
        var allMoves = piece.pseudoLegalMoves(p.board(), coords);
        if (piece instanceof King king) {
            var rights = p.castlingRights();
            if (king.getColor() == Color.WHITE) {
                if (rights.whiteKingSide()) allMoves.add(Move.castleKingSide(Color.WHITE));
                if (rights.whiteQueenSide()) allMoves.add(Move.castleQueenSide(Color.WHITE));
            } else {
                if (rights.blackKingSide()) allMoves.add(Move.castleKingSide(Color.BLACK));
                if (rights.blackQueenSide()) allMoves.add(Move.castleQueenSide(Color.BLACK));
            }
        }
        allMoves.removeIf(move -> !isLegal(p, move));
        return allMoves;
    }

    public List<Move> sideLegalMoves(Position p, Color c) {
        List<Move> legalMoves = new ArrayList<>();
        var pieces = p.board().getPiecesByColor(c);
        for (var entry : pieces.entrySet()) {
            legalMoves.addAll(pieceLegalMoves(p, entry.getKey()));
        }

        return legalMoves;
    }

    private int getKingRank(Color c) { return c == Color.WHITE ? 0 : 7; }
    private int getFinalCastlingPosition(MoveKind kind) { return kind == MoveKind.CASTLE_QUEEN_SIDE ? 2 : 6; }
    private int getRookFile(MoveKind kind) { return kind == MoveKind.CASTLE_QUEEN_SIDE ? 0 : 7; }
}
