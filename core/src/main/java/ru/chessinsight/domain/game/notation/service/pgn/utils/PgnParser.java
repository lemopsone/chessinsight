package ru.chessinsight.domain.game.notation.service.pgn.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PgnParser {
    public static PgnAst parse(String text){
        List<PgnTokenizer.Tok> toks = PgnTokenizer.tokenize(text);
        Cursor cur = new Cursor(toks);

        Map<String,String> tags = new LinkedHashMap<>();

        while (true) {
            PgnTokenizer.Tok tok = cur.peek();
            if (tok == null) {
                break;
            }

            if (tok instanceof PgnTokenizer.Tag t) {
                tags.put(t.key, t.value);
                cur.next();
                continue;
            }
            if (tok instanceof PgnTokenizer.Newline) {
                cur.next();
                continue;
            }
            if (tok instanceof PgnTokenizer.Comment) {
                cur.next();
                continue;
            }

            break;
        }

        PgnAst ast = new PgnAst();
        ast.tags = tags;
        ast.mainline = parseLine(cur);
        if (ast.result == null) {
            ast.result = "*";
        }
        return ast;
    }

    private static List<PgnAst.Node> parseLine(Cursor cur){
        List<PgnAst.Node> line = new ArrayList<>();
        List<String> pendingComments = new ArrayList<>();
        while (true) {
            var token = cur.peek();
            if (isEndOfLine(token)) {
                break;
            }
            if (consumeComment(token, pendingComments, cur)) {
                continue;
            }
            if (consumeSeparators(token, cur)) {
                continue;
            }
            if (token instanceof PgnTokenizer.LParen) {
                consumeVariation(cur, line);
                continue;
            }
            if (token instanceof PgnTokenizer.San san) {
                line.add(parseSanNode(cur, san, pendingComments));
                continue;
            }
            cur.next();
        }
        return line;
    }

    private static boolean isEndOfLine(PgnTokenizer.Tok token) {
        return token == null || token instanceof PgnTokenizer.ResultTok || token instanceof PgnTokenizer.RParen;
    }

    private static boolean consumeComment(PgnTokenizer.Tok token, List<String> pendingComments, Cursor cur) {
        if (token instanceof PgnTokenizer.Comment comment) {
            pendingComments.add(comment.text);
            cur.next();
            return true;
        }
        return false;
    }

    private static boolean consumeSeparators(PgnTokenizer.Tok token, Cursor cur) {
        if (token instanceof PgnTokenizer.Dot || token instanceof PgnTokenizer.Newline) {
            cur.next();
            return true;
        }
        return false;
    }

    private static void consumeVariation(Cursor cur, List<PgnAst.Node> line) {
        cur.next();
        List<PgnAst.Node> variation = parseLine(cur);
        if (cur.peek() instanceof PgnTokenizer.RParen) {
            cur.next();
        }
        if (line.isEmpty()) {
            return;
        }
        line.getLast().variations.add(variation);
    }

    private static PgnAst.Node parseSanNode(Cursor cur, PgnTokenizer.San san, List<String> pendingComments) {
        PgnAst.Node node = new PgnAst.Node(san.text);
        if (!pendingComments.isEmpty()) {
            node.commentBefore = String.join(" ", pendingComments);
            pendingComments.clear();
        }
        cur.next();
        consumeNodeTail(cur, node);
        return node;
    }

    private static void consumeNodeTail(Cursor cur, PgnAst.Node node) {
        while (true) {
            var token = cur.peek();
            if (isNodeTailEnd(token)) {
                return;
            }
            if (consumeSeparators(token, cur)) {
                continue;
            }
            if (token instanceof PgnTokenizer.Comment comment) {
                appendCommentAfter(node, comment.text);
                cur.next();
                continue;
            }
            if (token instanceof PgnTokenizer.Nag nag) {
                node.nags.add(nag.value);
                cur.next();
                continue;
            }
            cur.next();
        }
    }

    private static boolean isNodeTailEnd(PgnTokenizer.Tok token) {
        return token == null
                || token instanceof PgnTokenizer.San
                || token instanceof PgnTokenizer.ResultTok
                || token instanceof PgnTokenizer.RParen
                || token instanceof PgnTokenizer.LParen;
    }

    private static void appendCommentAfter(PgnAst.Node node, String commentText) {
        node.commentAfter = node.commentAfter == null ? commentText : (node.commentAfter + " " + commentText);
    }

    private static class Cursor {
        final List<PgnTokenizer.Tok> toks;
        int i = 0;

        Cursor(List<PgnTokenizer.Tok> t) {
            toks = t;
        }

        PgnTokenizer.Tok peek() {
            return i < toks.size() ? toks.get(i) : null;
        }

        void next() {
            if (i < toks.size()) {
                i++;
            }
        }
    }
}
