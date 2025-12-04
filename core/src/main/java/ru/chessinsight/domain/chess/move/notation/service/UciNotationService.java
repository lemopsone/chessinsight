package ru.chessinsight.domain.chess.move.notation.service;

import ru.chessinsight.domain.chess.move.model.Move;
import ru.chessinsight.domain.chess.move.service.MoveMaker;
import ru.chessinsight.domain.chess.move.service.MoveValidator;
import ru.chessinsight.domain.chess.piece.model.PromotionPieceType;
import ru.chessinsight.domain.chess.position.model.BoardCoordinates;
import ru.chessinsight.domain.chess.position.model.Position;
import ru.chessinsight.domain.exception.InvalidStateException;
import ru.chessinsight.domain.exception.InvalidUCIException;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.model.GameMove;
import ru.chessinsight.domain.game.model.GameResult;

import java.util.*;
import java.util.regex.Pattern;

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

        final String s = uci.trim().toLowerCase();
        if (s.length() != 4 && s.length() != 5) {
            throw new InvalidUCIException("UCI must be 4 or 5 chars " + uci);
        }

        final String fromSq = s.substring(0, 2);
        final String toSq   = s.substring(2, 4);
        final Character promoChar = (s.length() == 5) ? s.charAt(4) : null;

        final BoardCoordinates from = BoardCoordinates.fromString(fromSq);
        final BoardCoordinates to   = BoardCoordinates.fromString(toSq);
        final PromotionPieceType promo = promoChar == null ? null : PromotionPieceType.fromLetter(promoChar);

        final MoveValidator validator = new MoveValidator();
        final List<Move> legals = validator.sideLegalMoves(pos, pos.sideToMove());

        Move match = null;
        for (Move m : legals) {
            if (m.from() == null || m.to() == null) continue;
            if (m.from().rank() != from.rank() || m.from().file() != from.file()) continue;
            if (m.to().rank() != to.rank() || m.to().file() != to.file()) continue;

            if ((promo == null && m.promotionTo() != null) ||
                    (promo != null && (m.promotionTo() == null || m.promotionTo() != promo))) {
                continue;
            }

            if (match != null) {
                throw new InvalidUCIException("ambiguous UCI in this position " + uci);
            }
            match = m;
        }

        if (match == null) {
            throw new InvalidUCIException("no legal move matches UCI " + uci);
        }
        return match;
    }
}
