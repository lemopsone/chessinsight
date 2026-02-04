package ru.chessinsight.domain.chess.move.special;

import org.junit.jupiter.api.Test;
import ru.chessinsight.domain.chess.move.model.Move;
import ru.chessinsight.domain.chess.move.model.MoveKind;
import ru.chessinsight.domain.chess.move.notation.service.UciNotationService;
import ru.chessinsight.domain.chess.move.service.MoveMaker;
import ru.chessinsight.domain.chess.position.model.Position;
import ru.chessinsight.testutil.Tag;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class EnPassantTest {

    @Test
    void en_passant_is_recognized_and_applied() {
        Position start = Position.fromFEN("8/3p4/8/4P3/8/8/8/4k2K b - - 0 1");
        UciNotationService uci = new UciNotationService();

        Move d5 = uci.uciToMove("d7d5", start);
        Position afterBlack = MoveMaker.apply(start, d5);

        Move ep = uci.uciToMove("e5d6", afterBlack);
        assertEquals(MoveKind.EN_PASSANT, ep.kind());

        Position afterEp = MoveMaker.apply(afterBlack, ep);
        assertTrue(afterEp.toFEN().contains("3P4"));
    }
}