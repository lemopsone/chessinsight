package ru.chessinsight.domain.common.pagination;

import org.junit.jupiter.api.Test;

import java.util.List;
import io.qameta.allure.Tag;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class PageTest {

    @Test
    void totalPages_returnsZero_whenNoElements() {
        Page<String> page = new Page<>(List.of(), 0, 10, 0);

        assertEquals(0, page.totalPages());
    }

    @Test
    void totalPages_roundsUp() {
        Page<String> page = new Page<>(List.of(), 0, 10, 25);

        assertEquals(3, page.totalPages());
    }

    @Test
    void hasNext_returnsTrue_whenMorePages() {
        Page<String> page = new Page<>(List.of(), 0, 10, 25);

        assertTrue(page.hasNext());
    }

    @Test
    void hasNext_returnsFalse_onLastPage() {
        Page<String> page = new Page<>(List.of(), 2, 10, 25);

        assertFalse(page.hasNext());
    }

    @Test
    void hasPrevious_returnsFalse_onFirstPage() {
        Page<String> page = new Page<>(List.of(), 0, 10, 25);

        assertFalse(page.hasPrevious());
    }

    @Test
    void hasPrevious_returnsTrue_onLaterPage() {
        Page<String> page = new Page<>(List.of(), 1, 10, 25);

        assertTrue(page.hasPrevious());
    }
}