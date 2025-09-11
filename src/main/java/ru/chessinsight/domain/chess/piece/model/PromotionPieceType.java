package ru.chessinsight.domain.chess.piece.model;

public enum PromotionPieceType {
    QUEEN {
        @Override public Piece create(Color c) { return new Queen(c); }
    },
    ROOK {
        @Override public Piece create(Color c) { return new Rook(c); }
    },
    BISHOP {
        @Override public Piece create(Color c) { return new Bishop(c); }
    },
    KNIGHT {
        @Override public Piece create(Color c) { return new Knight(c); }
    };

    public abstract Piece create(Color c);

    public static PromotionPieceType fromLetter(char c) {
        return switch (c) {
            case 'Q' -> PromotionPieceType.QUEEN;
            case 'R' -> PromotionPieceType.ROOK;
            case 'B' -> PromotionPieceType.BISHOP;
            case 'N' -> PromotionPieceType.KNIGHT;
            default -> null;
        };
    }

    public char toLetter() {
        return switch (this) {
            case QUEEN -> 'Q';
            case ROOK -> 'R';
            case BISHOP -> 'B';
            case KNIGHT -> 'K';
        };
    }
}
