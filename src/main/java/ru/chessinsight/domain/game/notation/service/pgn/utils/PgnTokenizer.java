package ru.chessinsight.domain.game.notation.service.pgn.utils;

import java.util.ArrayList;
import java.util.List;

class PgnTokenizer {
    static abstract class Tok {}
    static class Tag extends Tok { final String key, value; Tag(String k, String v){this.key=k; this.value=v;} }
    static class ResultTok extends Tok { final String value; ResultTok(String v){this.value=v;} }
    static class LParen extends Tok {}
    static class RParen extends Tok {}
    static class Nag extends Tok { final int value; Nag(int v){this.value=v;} }
    static class Comment extends Tok { final String text; Comment(String t){this.text=t;} }
    static class Dot extends Tok { final int count; Dot(int c){this.count=c;} }
    static class San extends Tok { final String text; San(String t){this.text=t;} }
    static class Newline extends Tok {}

    private static boolean isSpace(char c){ return c==' '||c=='\t'||c=='\n'||c=='\r'||c=='\f'; }

    static List<Tok> tokenize(String input){
        String s = input.replace("\r\n", "\n").replace("\r", "\n");
        List<Tok> out = new ArrayList<>();
        int i=0, n=s.length();
        while(i<n){
            char c = s.charAt(i);
            if (isSpace(c)) { if (c=='\n') out.add(new Newline()); i++; continue; }
            if (c==';'){ int j=i+1; while(j<n && s.charAt(j)!='\n') j++; out.add(new Comment(s.substring(i+1,j).trim())); i=j; continue; }
            if (c=='{'){ int j=i+1; while(j<n && s.charAt(j)!='}') j++; String text = s.substring(i+1, Math.min(j,n)).replaceAll("\\s+"," ").trim(); out.add(new Comment(text)); i=Math.min(j+1,n); continue; }
            if (c=='['){ int j=i+1; while(j<n && s.charAt(j)!=']') j++; String raw = s.substring(i+1, Math.min(j,n)).trim(); String[] parts = raw.split("\\s+",2);
                if (parts.length==2 && parts[1].startsWith("\"") && parts[1].endsWith("\"")) {
                    String val = parts[1].substring(1, parts[1].length()-1);
                    out.add(new Tag(parts[0], val));
                }
                i=Math.min(j+1,n); continue; }
            if (c=='('){ out.add(new LParen()); i++; continue; }
            if (c==')'){ out.add(new RParen()); i++; continue; }
            if (c=='$'){ int j=i+1; while(j<n && Character.isDigit(s.charAt(j))) j++; String v=s.substring(i+1,j); try{ out.add(new Nag(Integer.parseInt(v))); }catch(Exception ignore){} i=j; continue; }
            if (Character.isDigit(c)){
                int j=i; while(j<n && Character.isDigit(s.charAt(j))) j++;
                int k=j; int dots=0; while(k<n && s.charAt(k)=='.'){ dots++; k++; }
                if (dots>0){ out.add(new Dot(dots)); i=k; continue; }
                String ahead = nextToken(s,i);
                if (ahead.matches("^(1-0|0-1|1/2-1/2|*)$")) { out.add(new ResultTok(ahead)); i+=ahead.length(); continue; }
                i=j; continue;
            }
            if (c=='1' || c=='0' || c=='*'){
                String ahead = nextToken(s,i);
                if (ahead.matches("^(1-0|0-1|1/2-1/2|*)$")) { out.add(new ResultTok(ahead)); i+=ahead.length(); continue; }
            }
            int j=i; while(j<n && !isSpace(s.charAt(j)) && s.charAt(j)!='(' && s.charAt(j)!=')') j++;
            String tok = s.substring(i,j);
            if (tok.equals("...")||tok.equals("..")) { out.add(new Dot(tok.length())); i=j; continue; }
            out.add(new San(tok)); i=j;
        }
        return out;
    }

    private static String nextToken(String s, int i){
        int j=i; int n=s.length();
        while(j<n && !isSpace(s.charAt(j))) j++;
        return s.substring(i,j);
    }
}
