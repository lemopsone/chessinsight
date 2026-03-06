package ru.chessinsight.domain.chess.move.notation.service;

import ru.chessinsight.domain.chess.position.model.BoardCoordinates;
import ru.chessinsight.domain.chess.position.model.Position;
import ru.chessinsight.domain.chess.move.model.Move;
import ru.chessinsight.domain.chess.move.model.MoveKind;
import ru.chessinsight.domain.chess.move.service.MoveMaker;
import ru.chessinsight.domain.chess.move.service.MoveValidator;
import ru.chessinsight.domain.chess.piece.model.Piece;
import ru.chessinsight.domain.exception.InvalidSANException;
import ru.chessinsight.domain.exception.InvalidStateException;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class SanNotationService {
    private static final String FILES = "abcdefgh";

    public Move sanToMove(String san, Position pos){
        if (san.equals("O-O") || san.equals("0-0")) {
            return Move.castleKingSide(pos.sideToMove());
        }
        if (san.equals("O-O-O") || san.equals("0-0-0")) {
            return Move.castleQueenSide(pos.sideToMove());
        }
        ParsedSan parsed = parseSan(san);
        MoveValidator validator = new MoveValidator();
        List<Move> legalMoves = validator.sideLegalMoves(pos, pos.sideToMove());
        List<Move> candidates = legalMoves.stream()
                .filter(move -> matchesSanMove(pos, move, parsed))
                .collect(Collectors.toList());
        List<Move> disambiguated = applyDisambiguation(candidates, parsed.disambiguation());
        return singleCandidateOrThrow(san, disambiguated);
    }

    public String moveToSan(Move move, Position before){
        if (move.kind()== MoveKind.CASTLE_KING_SIDE) return "O-O";
        if (move.kind()== MoveKind.CASTLE_QUEEN_SIDE) return "O-O-O";
        StringBuilder sb = new StringBuilder();
        String pieceLetter = letterOf(pieceOf(before, move.from()));
        boolean isPawn = pieceLetter.equals("P");
        appendPiecePrefix(sb, pieceLetter, isPawn);
        appendDisambiguation(sb, before, move, pieceLetter, isPawn);
        appendCaptureAndDestination(sb, move, isPawn);
        appendPromotion(sb, move);
        appendCheckSuffix(sb, before, move);
        return sb.toString();
    }

    private static ParsedSan parseSan(String san) {
        String stripped = stripCheckSuffix(san);
        PromotionPart promotionPart = splitPromotion(stripped);
        String noCapture = promotionPart.base().replace("x", "");
        String pieceLetter = detectPieceLetter(noCapture);
        String destination = noCapture.substring(noCapture.length() - 2);
        String disambiguation = noCapture.substring(pieceLetter.equals("P") ? 0 : 1, noCapture.length() - 2);
        boolean capture = promotionPart.base().contains("x");
        return new ParsedSan(pieceLetter, BoardCoordinates.fromString(destination), disambiguation, capture, promotionPart.promotion());
    }

    private static String stripCheckSuffix(String san) {
        if (san.endsWith("+") || san.endsWith("#")) {
            return san.substring(0, san.length() - 1);
        }
        return san;
    }

    private static PromotionPart splitPromotion(String san) {
        int eq = san.indexOf('=');
        if (eq >= 0 && eq == san.length() - 2) {
            return new PromotionPart(san.substring(0, san.length() - 2), san.substring(san.length() - 1));
        }
        return new PromotionPart(san, null);
    }

    private static String detectPieceLetter(String sanWithoutCapture) {
        char first = sanWithoutCapture.charAt(0);
        return "PNBRQK".indexOf(first) >= 0 ? String.valueOf(first) : "P";
    }

    private static boolean matchesSanMove(Position pos, Move move, ParsedSan parsed) {
        if (move.from() == null || move.to() == null) {
            return false;
        }
        if (!sameSquare(move.to(), parsed.destination())) {
            return false;
        }
        if (!letterOf(pieceOf(pos, move.from())).equals(parsed.pieceLetter())) {
            return false;
        }
        if (!matchesPromotion(move, parsed.promotion())) {
            return false;
        }
        return parsed.capture() == move.isCapture();
    }

    private static boolean sameSquare(BoardCoordinates left, BoardCoordinates right) {
        return left.file() == right.file() && left.rank() == right.rank();
    }

    private static boolean matchesPromotion(Move move, String promotion) {
        if (promotion == null) {
            return move.promotionTo() == null;
        }
        if (move.promotionTo() == null) {
            return false;
        }
        return move.promotionTo().name().startsWith(promotion);
    }

    private static List<Move> applyDisambiguation(List<Move> candidates, String disambiguation) {
        if (disambiguation.isEmpty()) {
            return candidates;
        }
        if (disambiguation.length() == 2) {
            int file = FILES.indexOf(disambiguation.charAt(0));
            int rank = Character.getNumericValue(disambiguation.charAt(1)) - 1;
            return candidates.stream()
                    .filter(move -> move.from() != null && move.from().file() == file && move.from().rank() == rank)
                    .collect(Collectors.toList());
        }
        char marker = disambiguation.charAt(0);
        if (Character.isLetter(marker)) {
            int file = FILES.indexOf(marker);
            return candidates.stream()
                    .filter(move -> move.from() != null && move.from().file() == file)
                    .collect(Collectors.toList());
        }
        int rank = Character.getNumericValue(marker) - 1;
        return candidates.stream()
                .filter(move -> move.from() != null && move.from().rank() == rank)
                .collect(Collectors.toList());
    }

    private static Move singleCandidateOrThrow(String san, List<Move> candidates) {
        if (candidates.size() == 1) {
            return candidates.getFirst();
        }
        throw new InvalidSANException("no legal moves match SAN " + san);
    }

    private static void appendPiecePrefix(StringBuilder sb, String pieceLetter, boolean isPawn) {
        if (!isPawn) {
            sb.append(pieceLetter);
        }
    }

    private static void appendDisambiguation(StringBuilder sb, Position before, Move move, String pieceLetter, boolean isPawn) {
        if (isPawn) {
            return;
        }
        List<Move> rivals = findRivals(before, move, pieceLetter);
        if (rivals.isEmpty()) {
            return;
        }
        boolean fileUnique = rivals.stream().allMatch(rival -> rival.from() == null || rival.from().file() != move.from().file());
        boolean rankUnique = rivals.stream().allMatch(rival -> rival.from() == null || rival.from().rank() != move.from().rank());
        if (fileUnique) {
            sb.append(fileChar(move.from().file()));
            return;
        }
        if (rankUnique) {
            sb.append(move.from().rank() + 1);
            return;
        }
        sb.append(fileChar(move.from().file())).append(move.from().rank() + 1);
    }

    private static List<Move> findRivals(Position before, Move move, String pieceLetter) {
        MoveValidator validator = new MoveValidator();
        return validator.sideLegalMoves(before, before.sideToMove()).stream()
                .filter(candidate -> !candidate.equals(move))
                .filter(candidate -> candidate.to() != null && move.to() != null && sameSquare(candidate.to(), move.to()))
                .filter(candidate -> candidate.promotionTo() == move.promotionTo())
                .filter(candidate -> letterOf(pieceOf(before, candidate.from())).equals(pieceLetter))
                .toList();
    }

    private static void appendCaptureAndDestination(StringBuilder sb, Move move, boolean isPawn) {
        if (move.from() == null || move.to() == null) {
            throw new InvalidStateException("invalid Move state (to/from is null)");
        }
        if (isPawn && move.isCapture()) {
            sb.append(fileChar(move.from().file()));
        }
        if (move.isCapture()) {
            sb.append('x');
        }
        sb.append(fileChar(move.to().file())).append(move.to().rank() + 1);
    }

    private static void appendPromotion(StringBuilder sb, Move move) {
        if (move.promotionTo() != null) {
            sb.append('=').append(move.promotionTo().name().charAt(0));
        }
    }

    private static void appendCheckSuffix(StringBuilder sb, Position before, Move move) {
        Position after = MoveMaker.apply(before, move);
        MoveValidator validator = new MoveValidator();
        boolean inCheck = validator.isKingInCheck(after, after.sideToMove());
        boolean checkmate = inCheck && validator.sideLegalMoves(after, after.sideToMove()).isEmpty();
        if (checkmate) {
            sb.append('#');
        } else if (inCheck) {
            sb.append('+');
        }
    }

    private record PromotionPart(String base, String promotion) {}

    private record ParsedSan(
            String pieceLetter,
            BoardCoordinates destination,
            String disambiguation,
            boolean capture,
            String promotion
    ) {}

    public static String letterOf(String piece){
        if (piece==null) return "";
        return switch (piece.toUpperCase(Locale.ROOT)) {
            case "PAWN" -> "P";
            case "KNIGHT" -> "N";
            case "BISHOP" -> "B";
            case "ROOK" -> "R";
            case "QUEEN" -> "Q";
            case "KING" -> "K";
            default -> "";
        };
    }

    private static String pieceOf(Position pos, BoardCoordinates sq){
        Piece pc = pos.board().at(sq);
        return pc==null? null : pc.getClass().getSimpleName();
    }

    private static char fileChar(int f){ return "abcdefgh".charAt(f); }
}
