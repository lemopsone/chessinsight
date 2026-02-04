package ru.chessinsight.domain.common.pagination;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PageParamsTest {

    @Test
    void offset_returnsPageSizeProduct() {
        PageParams params = new PageParams(2, 10);

        assertEquals(20, params.offset());
    }

    @Test
    void offset_returnsZero_forFirstPage() {
        PageParams params = new PageParams(0, 5);

        assertEquals(0, params.offset());
    }

    @Test
    void constructor_throws_forNegativePage() {
        assertThrows(IllegalArgumentException.class, () -> new PageParams(-1, 10));
    }

    @Test
    void constructor_throws_forInvalidSize() {
        assertThrows(IllegalArgumentException.class, () -> new PageParams(0, 0));
    }
}