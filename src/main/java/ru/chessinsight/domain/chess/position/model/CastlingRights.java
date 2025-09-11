package ru.chessinsight.domain.chess.position.model;

public record CastlingRights(
        boolean whiteKingSide, boolean whiteQueenSide,
        boolean blackKingSide, boolean blackQueenSide
) {
    public static CastlingRights all() { return new CastlingRights(true, true, true, true); }
    public static CastlingRights none() { return new CastlingRights(false, false, false, false); }

    public String toFEN() {
        String rights = (whiteKingSide ? "K" : "") +
                        (whiteQueenSide ? "Q" : "") +
                        (blackKingSide ? "k" : "") +
                        (blackQueenSide ? "q" : "");
        if (rights.isEmpty())
            return "-";
        return rights;
    }

    public static CastlingRights fromFEN(String FEN) {
        if (FEN.equals("-"))
            return CastlingRights.none();
        boolean WK, WQ, BK, BQ;
        WK = FEN.contains("K");
        WQ = FEN.contains("Q");
        BK = FEN.contains("k");
        BQ = FEN.contains("q");
        return new CastlingRights(WK, WQ, BK, BQ);
    }
}
