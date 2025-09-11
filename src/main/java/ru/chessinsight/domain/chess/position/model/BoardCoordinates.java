package ru.chessinsight.domain.chess.position.model;

import ru.chessinsight.domain.exception.BadBoardCoordinatesException;

public record BoardCoordinates(int rank, int file) {
    public static BoardCoordinates fromString(String data) {
        var arr = data.toCharArray();
        if (arr.length != 2)
            throw new BadBoardCoordinatesException("expected length of 2, received " + arr.length);
        if ((arr[0] < 'a' || arr[0] > 'h')
            || (arr[1] < '1' || arr[1] > '8')) {
            throw new BadBoardCoordinatesException("expected [a-h][1-8], got " + data);
        }
        int file = arr[0] - 'a';
        int rank = arr[1] - '1';
        return new BoardCoordinates(rank, file);
    }

    @Override
    public String toString() {
        return "" + (char)('a' + file) + (char)('1' + rank);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BoardCoordinates that = (BoardCoordinates) o;

        if (rank != that.rank) return false;
        return file == that.file;
    }

    @Override
    public int hashCode() {
        int result = rank;
        result = 31 * result + file;
        return result;
    }
}
