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
        return new BoardCoordinates(arr[1] - '1', arr[0] - 'a');
    }

    @Override
    public String toString() {
        return "" + ('a' + file) + ('1' + rank);
    }
}
