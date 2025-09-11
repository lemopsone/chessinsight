package ru.chessinsight.domain.game.notation.service.pgn.utils;

import java.util.*;

public class PgnParser {
    public static PgnAst parse(String text){
        List<PgnTokenizer.Tok> toks = PgnTokenizer.tokenize(text);
        Cursor cur = new Cursor(toks);
        Map<String,String> tags = new LinkedHashMap<>();
        while (cur.peek() instanceof PgnTokenizer.Tag t){
            tags.put(t.key, t.value);
            cur.next();
        }
        PgnAst ast = new PgnAst();
        ast.tags = tags;
        ast.mainline = parseLine(cur);
        if (cur.peek() instanceof PgnTokenizer.ResultTok r){ ast.result = r.value; cur.next(); }
        if (ast.result == null) ast.result = "*";
        return ast;
    }

    private static List<PgnAst.Node> parseLine(Cursor cur){
        List<PgnAst.Node> line = new ArrayList<>();
        List<String> pendingComments = new ArrayList<>();
        while(true){
            var t = cur.peek();
            if (t==null || t instanceof PgnTokenizer.ResultTok || t instanceof PgnTokenizer.RParen) break;
            if (t instanceof PgnTokenizer.Comment c){ pendingComments.add(c.text); cur.next(); continue; }
            if (t instanceof PgnTokenizer.Dot || t instanceof PgnTokenizer.Newline){ cur.next(); continue; }
            if (t instanceof PgnTokenizer.LParen){
                cur.next();
                List<PgnAst.Node> varLine = parseLine(cur);
                if (cur.peek() instanceof PgnTokenizer.RParen) cur.next();
                if (!line.isEmpty()){
                    var last = line.getLast();
                    last.variations.add(varLine);
                }
                continue;
            }
            if (t instanceof PgnTokenizer.San s){
                PgnAst.Node node = new PgnAst.Node(s.text);
                if (!pendingComments.isEmpty()){ node.commentBefore = String.join(" ", pendingComments); pendingComments.clear(); }
                cur.next();
                while(true){
                    var u = cur.peek();
                    if (u==null || u instanceof PgnTokenizer.San || u instanceof PgnTokenizer.ResultTok || u instanceof PgnTokenizer.RParen || u instanceof PgnTokenizer.LParen) break;
                    if (u instanceof PgnTokenizer.Dot || u instanceof PgnTokenizer.Newline){ cur.next(); continue; }
                    if (u instanceof PgnTokenizer.Comment c){ node.commentAfter = node.commentAfter==null ? c.text : (node.commentAfter + " " + c.text); cur.next(); continue; }
                    if (u instanceof PgnTokenizer.Nag n){ node.nags.add(n.value); cur.next(); continue; }
                    cur.next();
                }
                line.add(node);
                continue;
            }
            cur.next();
        }
        return line;
    }

    private static class Cursor{
        final List<PgnTokenizer.Tok> toks; int i=0;
        Cursor(List<PgnTokenizer.Tok> t){ toks=t; }
        PgnTokenizer.Tok peek(){ return i<toks.size()? toks.get(i): null; }
        PgnTokenizer.Tok next(){ return i<toks.size()? toks.get(i++): null; }
    }
}
