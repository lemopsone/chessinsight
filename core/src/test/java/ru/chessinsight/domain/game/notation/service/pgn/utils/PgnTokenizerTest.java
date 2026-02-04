package ru.chessinsight.domain.game.notation.service.pgn.utils;

import org.junit.jupiter.api.Test;
import java.util.List;
import io.qameta.allure.Tag;
import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class PgnTokenizerTest {
    @Test
    void tokenizes_tags_moves_comments_nags_result() {
        String pgn = """
            [Event "Friendly"]
            [Site "Here"]
            1. e4 e5 2. Nf3 {hello} Nc6 3. Bb5 $1 a6 1-0
            """;
        List<PgnTokenizer.Tok> toks = PgnTokenizer.tokenize(pgn);
        assertTrue(toks.stream().anyMatch(t -> t instanceof PgnTokenizer.Tag));
        assertTrue(toks.stream().anyMatch(t -> t instanceof PgnTokenizer.San));
        assertTrue(toks.stream().anyMatch(t -> t instanceof PgnTokenizer.Comment));
        assertTrue(toks.stream().anyMatch(t -> t instanceof PgnTokenizer.Nag));
        assertTrue(toks.stream().anyMatch(t -> t instanceof PgnTokenizer.ResultTok));
    }

    @Test
    void tokenizes_variations_and_dots() {
        String pgn = "1. e4 (1... c5 2. Nf3) 1... e5 2. Nf3 ... Nc6 *";
        List<PgnTokenizer.Tok> toks = PgnTokenizer.tokenize(pgn);
        assertTrue(toks.stream().anyMatch(t -> t instanceof PgnTokenizer.LParen));
        assertTrue(toks.stream().anyMatch(t -> t instanceof PgnTokenizer.RParen));
        assertTrue(toks.stream().anyMatch(t -> t instanceof PgnTokenizer.Dot));
    }
}