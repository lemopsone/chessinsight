package ru.chessinsight.domain.chess.position.model;

import org.junit.jupiter.api.Test;
import io.qameta.allure.Tag;
import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class BoardCoordinatesTest {
    @Test
    void fromString_and_toString_roundTrip() {
        var bc = BoardCoordinates.fromString("e4");
        assertEquals(3, bc.rank());
        assertEquals(4, bc.file());
        assertEquals("e4", bc.toString());
    }

    @Test
    void fromString_rejects_bad_input() {
        assertThrows(RuntimeException.class, () -> BoardCoordinates.fromString("z9"));
        assertThrows(RuntimeException.class, () -> BoardCoordinates.fromString("e"));
        assertThrows(RuntimeException.class, () -> BoardCoordinates.fromString("e44"));
    }
}