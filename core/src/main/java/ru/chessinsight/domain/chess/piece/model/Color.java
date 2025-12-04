package ru.chessinsight.domain.chess.piece.model;

public enum Color {
    WHITE,
    BLACK;
    public Color opponent() { return this == WHITE ? BLACK : WHITE; }
    public String toString() {
        return switch (this) {
            case WHITE -> "(white)";
            case BLACK -> "(black)";
        };
    }
}
