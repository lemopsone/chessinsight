package ru.chessinsight.domain.game.notation.service.pgn.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PgnAst {
    public static class Node {
        public final String san;
        public final List<Integer> nags = new ArrayList<>();
        public String commentBefore;
        public String commentAfter;
        public final List<List<Node>> variations = new ArrayList<>();
        public Node(String san) { this.san = san; }
    }
    public Map<String,String> tags;
    public List<Node> mainline = new ArrayList<>();
    public String result = "*";
}
