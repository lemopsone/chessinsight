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
            return Move.castleKingSide();
        }
        if (san.equals("O-O-O") || san.equals("0-0-0")) {
            return Move.castleQueenSide();
        }
        String s = san;
        boolean checkOrMate = s.endsWith("+") || s.endsWith("#");
        if (checkOrMate) s = s.substring(0, s.length()-1);
        String promo;
        int eq = s.indexOf('=');
        if (eq>=0 && eq==s.length()-2){
            promo = s.substring(s.length()-1);
            s = s.substring(0, s.length()-2);
        } else {
            promo = null;
        }
        boolean capture = s.contains("x");
        s = s.replace("x","");

        char first = s.charAt(0);
        String pieceLetter = "PNBRQK".indexOf(first)>=0 ? String.valueOf(first) : "P";
        String dest = s.substring(s.length()-2);
        String disamb = s.substring(pieceLetter.equals("P")?0:1, s.length()-2);

        BoardCoordinates to = BoardCoordinates.fromString(dest);
        MoveValidator validator = new MoveValidator();
        List<Move> legals = validator.sideLegalMoves(pos, pos.sideToMove());
        List<Move> cands = legals.stream().filter(m -> {
            if (m.to()==null) return false;
            if (m.to().file()!=to.file() || m.to().rank()!=to.rank()) return false;
            String p = pieceOf(pos, m.from());
            if (!letterOf(p).equals(pieceLetter)) return false;
            if (promo != null){
                if (m.promotionTo()==null) return false;
                if (!m.promotionTo().name().substring(0,1).equals(promo)) return false;
            }
            if (capture && !m.isCapture()) return false;
            return capture || !m.isCapture();
        }).collect(Collectors.toList());

        if (!disamb.isEmpty()){
            if (disamb.length()==2){
                int f = FILES.indexOf(disamb.charAt(0));
                int r = Character.getNumericValue(disamb.charAt(1))-1;
                cands = cands.stream().filter(m -> m.from().file()==f && m.from().rank()==r).collect(Collectors.toList());
            } else if (Character.isLetter(disamb.charAt(0))) {
                int f = FILES.indexOf(disamb.charAt(0));
                cands = cands.stream().filter(m -> m.from().file()==f).collect(Collectors.toList());
            } else {
                int r = Character.getNumericValue(disamb.charAt(0))-1;
                cands = cands.stream().filter(m -> m.from().rank()==r).collect(Collectors.toList());
            }
        }
        if (cands.size()==1) return cands.getFirst();
        throw new InvalidSANException("no legal moves match SAN " + san);
    }

    public String moveToSan(Move move, Position before){
        if (move.kind()== MoveKind.CASTLE_KING_SIDE) return "O-O";
        if (move.kind()== MoveKind.CASTLE_QUEEN_SIDE) return "O-O-O";
        StringBuilder sb = new StringBuilder();
        String piece = letterOf(pieceOf(before, move.from()));
        boolean isPawn = piece.equals("P");
        if (!isPawn) sb.append(piece);
        MoveValidator validator = new MoveValidator();
        List<Move> rivals = validator.sideLegalMoves(before, before.sideToMove()).stream()
                .filter(m -> m != move && m.to() != null && move.to() != null
                        && m.promotionTo() == move.promotionTo()
                        && m.to().file() == move.to().file() && m.to().rank() == move.to().rank()
                        && letterOf(pieceOf(before, m.from())).equals(piece))
                .toList();
        if (!rivals.isEmpty()){
            boolean fileUnique = rivals.stream().allMatch(m -> m.from().file()!=move.from().file());
            boolean rankUnique = rivals.stream().allMatch(m -> m.from().rank()!=move.from().rank());
            if (!isPawn) {
                if (fileUnique) sb.append(fileChar(move.from().file()));
                else if (rankUnique) sb.append(move.from().rank() + 1);
                else sb.append(fileChar(move.from().file())).append(move.from().rank() + 1);
            }
        }
        boolean capture = move.isCapture();
        if (isPawn && capture){
            sb.append(fileChar(move.from().file()));
        }
        if (capture) sb.append('x');
        if (move.from() == null || move.to() == null)
            throw new InvalidStateException("invalid Move state (to/from is null)");
        sb.append(fileChar(move.to().file())).append(move.to().rank()+1);
        if (move.promotionTo()!=null){
            sb.append('=').append(move.promotionTo().name().charAt(0));
        }
        Position after = MoveMaker.apply(before, move);
        MoveValidator v = new MoveValidator();
        var legalMovesAfter = v.sideLegalMoves(after, after.sideToMove());
        boolean inCheck = v.isKingInCheck(after, after.sideToMove());
        boolean checkmate = inCheck && legalMovesAfter.isEmpty();
        if (checkmate) sb.append('#');
        else if (inCheck) sb.append('+');
        return sb.toString();
    }

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
