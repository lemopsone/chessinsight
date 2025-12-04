package ru.chessinsight.domain.game.notation.service.pgn;

import ru.chessinsight.domain.chess.move.notation.service.SanNotationService;
import ru.chessinsight.domain.chess.move.notation.service.UciNotationService;
import ru.chessinsight.domain.chess.position.model.Position;
import ru.chessinsight.domain.chess.move.model.Move;
import ru.chessinsight.domain.chess.move.service.MoveMaker;
import ru.chessinsight.domain.exception.InvalidSANException;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.model.GameMove;
import ru.chessinsight.domain.game.notation.service.pgn.utils.PgnAst;
import ru.chessinsight.domain.game.notation.service.pgn.utils.PgnParser;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PgnService {
    private final SanNotationService sanNotationService;
    private final UciNotationService uciNotationService;
    public PgnService() {
        sanNotationService = new SanNotationService();
        uciNotationService = new UciNotationService();
    }

    public PgnAst parse(String pgnText){
        return PgnParser.parse(pgnText);
    }

    public Game createEmptyGameFromPGN(String pgnText) {
        PgnAst ast = parse(pgnText);
        Game game = new Game();
        game.setPgn(pgnText);
        game.setEvent(ast.tags.get("Event"));
        game.setSite(ast.tags.get("Site"));
        game.setWhiteName(ast.tags.get("White"));
        game.setBlackName(ast.tags.get("Black"));
        game.setRound(ast.tags.get("Round"));
        String dateString = ast.tags.get("Date");
        game.setDate(dateString == null
                ? null
                : LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy.MM.dd")));
        return game;
    }

    public List<GameMove> extractMovesFromGameMainline(Game game){
        var pgnText = game.getPgn();
        PgnAst ast = parse(pgnText);
        Position pos = ast.tags!=null && "1".equals(ast.tags.get("SetUp")) && ast.tags.get("FEN")!=null
                ? Position.fromFEN(ast.tags.get("FEN"))
                : Position.initial();

        List<GameMove> moves = new ArrayList<>();

        for (PgnAst.Node n : ast.mainline){
            Move mv = sanNotationService.sanToMove(n.san, pos);
            if (mv==null) throw new InvalidSANException("cannot resolve SAN: " + n.san);
            String uci = uciNotationService.moveToUci(mv);
            String fenBefore = pos.toFEN();
            pos = MoveMaker.apply(pos, mv);
            GameMove gm = new GameMove(
                    null,
                    (moves.size()+1),
                    n.san, uci, fenBefore,
                    n.commentBefore, n.commentAfter, null);
            moves.add(gm);
        }
        return moves;
    }

    public String toPgn(Game game, List<GameMove> gameMoves){
        StringBuilder sb = new StringBuilder();
        sb.append("[Event \"?\"]\n");
        sb.append("[Site \"?\"]\n");
        sb.append("[Date \"").append(java.time.LocalDate.now()).append("\"]\n");
        sb.append("[Round \"-\"]\n");
        sb.append("[White \"?\"]\n");
        sb.append("[Black \"?\"]\n");
        sb.append("[Result \"*\"]\n\n");

        Position pos = Position.initial();
        int moveNo = 1;
        boolean white = true;
        for (GameMove gm : gameMoves){
            if (white){ sb.append(moveNo).append(". "); }
            sb.append(gm.getSan()).append(" ");
            Move mv = sanNotationService.sanToMove(gm.getSan(), pos);
            if (mv != null) pos = MoveMaker.apply(pos, mv);
            if (!white) moveNo++;
            white = !white;
        }
        sb.append("*");
        return sb.toString();
    }
}
