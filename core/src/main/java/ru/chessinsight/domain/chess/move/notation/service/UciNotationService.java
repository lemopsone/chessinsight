package ru.chessinsight.domain.chess.move.notation.service;

import ru.chessinsight.domain.chess.move.model.Move;
import ru.chessinsight.domain.chess.move.service.MoveValidator;
import ru.chessinsight.domain.chess.piece.model.PromotionPieceType;
import ru.chessinsight.domain.chess.position.model.BoardCoordinates;
import ru.chessinsight.domain.chess.position.model.Position;
import ru.chessinsight.domain.exception.InvalidStateException;
import ru.chessinsight.domain.exception.InvalidUCIException;

import java.util.List;

public final class UciNotationService {
    public String moveToUci(Move move) {
        if (move == null) throw new InvalidStateException("move is null");
        final String from = move.from().toString();
        final String to   = move.to().toString();
        final String promo = "" + (move.promotionTo() == null ? "" : move.promotionTo().toLetter());
        return (from + to + promo).toLowerCase();
    }

    public Move uciToMove(String uci, Position pos) {
        if (uci == null || uci.isBlank()) throw new InvalidUCIException("uci is empty");
        if (pos == null) throw new InvalidStateException("position is null");

        ParsedUci parsed = parseUci(uci);
        final MoveValidator validator = new MoveValidator();
        final List<Move> legals = validator.sideLegalMoves(pos, pos.sideToMove());
        return findMatchingMove(uci, legals, parsed);
    }

    private static ParsedUci parseUci(String uci) {
        final String normalized = uci.trim().toLowerCase();
        if (normalized.length() != 4 && normalized.length() != 5) {
            throw new InvalidUCIException("UCI must be 4 or 5 chars " + uci);
        }
        final BoardCoordinates from = BoardCoordinates.fromString(normalized.substring(0, 2));
        final BoardCoordinates to = BoardCoordinates.fromString(normalized.substring(2, 4));
        final PromotionPieceType promotion = normalized.length() == 5
                ? PromotionPieceType.fromLetter(normalized.charAt(4))
                : null;
        return new ParsedUci(from, to, promotion);
    }

    private static Move findMatchingMove(String rawUci, List<Move> legalMoves, ParsedUci parsed) {
        Move match = null;
        for (Move move : legalMoves) {
            if (!isSameEndpoints(move, parsed)) {
                continue;
            }
            if (!isSamePromotion(move, parsed.promotion())) {
                continue;
            }
            if (match != null) {
                throw new InvalidUCIException("ambiguous UCI in this position " + rawUci);
            }
            match = move;
        }
        if (match == null) {
            throw new InvalidUCIException("no legal move matches UCI " + rawUci);
        }
        return match;
    }

    private static boolean isSameEndpoints(Move move, ParsedUci parsed) {
        if (move.from() == null || move.to() == null) {
            return false;
        }
        return move.from().rank() == parsed.from().rank()
                && move.from().file() == parsed.from().file()
                && move.to().rank() == parsed.to().rank()
                && move.to().file() == parsed.to().file();
    }

    private static boolean isSamePromotion(Move move, PromotionPieceType promotion) {
        if (promotion == null) {
            return move.promotionTo() == null;
        }
        return move.promotionTo() == promotion;
    }

    private record ParsedUci(BoardCoordinates from, BoardCoordinates to, PromotionPieceType promotion) {}
}
