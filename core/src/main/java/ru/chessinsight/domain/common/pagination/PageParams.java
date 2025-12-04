package ru.chessinsight.domain.common.pagination;

public record PageParams(int page, int size) {

    public PageParams {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size <= 0 || size > 1000) {
            throw new IllegalArgumentException("size must be between 1 and 1000");
        }
    }

    public int offset() {
        return page * size;
    }
}
