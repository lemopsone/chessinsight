package ru.chessinsight.domain.common.pagination;

import java.util.List;

public record Page<T>(
        List<T> content,
        int page,
        int size,
        long totalElements
) {
    public int totalPages() {
        if (size == 0) return 0;
        return (int) Math.ceil((double) totalElements / (double) size);
    }

    public boolean hasNext() {
        return page + 1 < totalPages();
    }

    public boolean hasPrevious() {
        return page > 0;
    }
}
