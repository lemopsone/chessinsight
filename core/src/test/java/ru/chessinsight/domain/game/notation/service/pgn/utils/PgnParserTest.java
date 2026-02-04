package ru.chessinsight.domain.game.notation.service.pgn.utils;

import org.junit.jupiter.api.Test;
import ru.chessinsight.testutil.Tag;
import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class PgnParserTest {

    @Test
    void parses_tags_mainline_comments_nags_variations() {
        String pgn = """
            [Event "Friendly"]
            [Site "Here"]
            [Date "2025.01.01"]
            1. e4 e5 (1... c5 2. Nf3) 2. Nf3 {hi} Nc6 $2 3. Bb5 *
            """;
        PgnAst ast = PgnParser.parse(pgn);
        assertEquals("Friendly", ast.tags.get("Event"));
        assertEquals(5, ast.mainline.size());
        assertEquals("*", ast.result);
        assertEquals("Nf3", ast.mainline.get(2).san);
        assertEquals("hi", ast.mainline.get(2).commentAfter);
        assertTrue(ast.mainline.get(3).nags.contains(2));
        assertFalse(ast.mainline.get(1).variations.isEmpty());
        assertFalse(ast.mainline.get(1).variations.getFirst().isEmpty());
        assertEquals("c5", ast.mainline.get(1).variations.getFirst().getFirst().san);
    }

    @Test
    void parse_setsDefaultResult_whenMissing() {
        String pgn = "1. e4 e5 2. Nf3 Nc6";
        PgnAst ast = PgnParser.parse(pgn);

        assertEquals("*", ast.result);
        assertEquals(4, ast.mainline.size());
    }
}