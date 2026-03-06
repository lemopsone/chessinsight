package ru.chessinsight.domain.game.notation.service.pgn.utils;

import java.util.ArrayList;
import java.util.List;

class PgnTokenizer {
    static abstract class Tok {
    }

    static class Tag extends Tok {
        final String key;
        final String value;

        Tag(String k, String v) {
            this.key = k;
            this.value = v;
        }
    }

    static class ResultTok extends Tok {
        final String value;

        ResultTok(String v) {
            this.value = v;
        }
    }

    static class LParen extends Tok {
    }

    static class RParen extends Tok {
    }

    static class Nag extends Tok {
        final int value;

        Nag(int v) {
            this.value = v;
        }
    }

    static class Comment extends Tok {
        final String text;

        Comment(String t) {
            this.text = t;
        }
    }

    static class Dot extends Tok {
        final int count;

        Dot(int c) {
            this.count = c;
        }
    }

    static class San extends Tok {
        final String text;

        San(String t) {
            this.text = t;
        }
    }

    static class Newline extends Tok {
    }

    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    static List<Tok> tokenize(String input) {
        String s = input.replace("\r\n", "\n").replace("\r", "\n");
        List<Tok> out = new ArrayList<>();
        int i = 0;
        int n = s.length();
        while (i < n) {
            int wsNext = consumeWhitespace(s, i, out);
            if (wsNext != i) {
                i = wsNext;
                continue;
            }

            ParseOutcome structured = consumeStructuredToken(s, i, out);
            if (structured.consumed()) {
                i = structured.nextIndex();
                continue;
            }

            ParseOutcome numeric = consumeNumericOrResultToken(s, i, out);
            if (numeric.consumed()) {
                i = numeric.nextIndex();
                continue;
            }

            i = consumeSanLikeToken(s, i, out);
        }
        return out;
    }

    private static int consumeWhitespace(String s, int index, List<Tok> out) {
        char c = s.charAt(index);
        if (!isSpace(c)) {
            return index;
        }
        if (c == '\n') {
            out.add(new Newline());
        }
        return index + 1;
    }

    private static ParseOutcome consumeStructuredToken(String s, int index, List<Tok> out) {
        char c = s.charAt(index);
        if (c == ';') {
            return consumeLineComment(s, index, out);
        }
        if (c == '{') {
            return consumeBlockComment(s, index, out);
        }
        if (c == '[') {
            return consumeTag(s, index, out);
        }
        if (c == '(') {
            out.add(new LParen());
            return ParseOutcome.consumed(index + 1);
        }
        if (c == ')') {
            out.add(new RParen());
            return ParseOutcome.consumed(index + 1);
        }
        if (c == '$') {
            return consumeNag(s, index, out);
        }
        return ParseOutcome.notConsumed(index);
    }

    private static ParseOutcome consumeLineComment(String s, int index, List<Tok> out) {
        int end = index + 1;
        while (end < s.length() && s.charAt(end) != '\n') {
            end++;
        }
        out.add(new Comment(s.substring(index + 1, end).trim()));
        return ParseOutcome.consumed(end);
    }

    private static ParseOutcome consumeBlockComment(String s, int index, List<Tok> out) {
        int end = index + 1;
        while (end < s.length() && s.charAt(end) != '}') {
            end++;
        }
        String text = s.substring(index + 1, Math.min(end, s.length())).replaceAll("\\s+", " ").trim();
        out.add(new Comment(text));
        return ParseOutcome.consumed(Math.min(end + 1, s.length()));
    }

    private static ParseOutcome consumeTag(String s, int index, List<Tok> out) {
        int end = index + 1;
        while (end < s.length() && s.charAt(end) != ']') {
            end++;
        }
        String raw = s.substring(index + 1, Math.min(end, s.length())).trim();
        String[] parts = raw.split("\\s+", 2);
        if (parts.length == 2 && parts[1].startsWith("\"") && parts[1].endsWith("\"")) {
            String value = parts[1].substring(1, parts[1].length() - 1);
            out.add(new Tag(parts[0], value));
        }
        return ParseOutcome.consumed(Math.min(end + 1, s.length()));
    }

    private static ParseOutcome consumeNag(String s, int index, List<Tok> out) {
        int end = index + 1;
        while (end < s.length() && Character.isDigit(s.charAt(end))) {
            end++;
        }
        String value = s.substring(index + 1, end);
        try {
            out.add(new Nag(Integer.parseInt(value)));
        } catch (Exception ignore) {
            // keep tokenizer resilient for malformed NAG tokens
        }
        return ParseOutcome.consumed(end);
    }

    private static ParseOutcome consumeNumericOrResultToken(String s, int index, List<Tok> out) {
        char c = s.charAt(index);
        if (Character.isDigit(c)) {
            return consumeDigitLeadingToken(s, index, out);
        }
        if (isResultLead(c)) {
            return consumeStandaloneResultToken(s, index, out);
        }
        return ParseOutcome.notConsumed(index);
    }

    private static ParseOutcome consumeDigitLeadingToken(String s, int index, List<Tok> out) {
        int digitEnd = index;
        while (digitEnd < s.length() && Character.isDigit(s.charAt(digitEnd))) {
            digitEnd++;
        }
        int k = digitEnd;
        int dots = 0;
        while (k < s.length() && s.charAt(k) == '.') {
            dots++;
            k++;
        }
        if (dots > 0) {
            out.add(new Dot(dots));
            return ParseOutcome.consumed(k);
        }
        String ahead = nextToken(s, index);
        if (isResultToken(ahead)) {
            out.add(new ResultTok(ahead));
            return ParseOutcome.consumed(index + ahead.length());
        }
        return ParseOutcome.consumed(digitEnd);
    }

    private static ParseOutcome consumeStandaloneResultToken(String s, int index, List<Tok> out) {
        String ahead = nextToken(s, index);
        if (isResultToken(ahead)) {
            out.add(new ResultTok(ahead));
            return ParseOutcome.consumed(index + ahead.length());
        }
        return ParseOutcome.notConsumed(index);
    }

    private static boolean isResultLead(char c) {
        return c == '1' || c == '0' || c == '*';
    }

    private static boolean isResultToken(String token) {
        return token.equals("1-0")
                || token.equals("0-1")
                || token.equals("1/2-1/2")
                || token.equals("*");
    }

    private static int consumeSanLikeToken(String s, int index, List<Tok> out) {
        int end = index;
        while (end < s.length() && !isSpace(s.charAt(end)) && s.charAt(end) != '(' && s.charAt(end) != ')') {
            end++;
        }
        String token = s.substring(index, end);
        if (token.equals("...") || token.equals("..")) {
            out.add(new Dot(token.length()));
        } else {
            out.add(new San(token));
        }
        return end;
    }

    private static String nextToken(String s, int i) {
        int j = i;
        int n = s.length();
        while (j < n && !isSpace(s.charAt(j))) {
            j++;
        }
        return s.substring(i, j);
    }

    private record ParseOutcome(boolean consumed, int nextIndex) {
        private static ParseOutcome consumed(int nextIndex) {
            return new ParseOutcome(true, nextIndex);
        }

        private static ParseOutcome notConsumed(int nextIndex) {
            return new ParseOutcome(false, nextIndex);
        }
    }
}
