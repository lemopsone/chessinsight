package ru.chessinsight.testdata;

import ru.chessinsight.domain.game.model.GameMove;
import ru.chessinsight.domain.game.model.GameMoveAnalysis;

import java.util.UUID;

public class GameMoveBuilder {
    private UUID id = UUID.randomUUID();
    private int plyIndex = 1;
    private String san = "e4";
    private String uci = "e2e4";
    private String positionFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    private String commentBefore;
    private String commentAfter;
    private GameMoveAnalysis analysis;

    public static GameMoveBuilder move() {
        return new GameMoveBuilder();
    }

    public GameMoveBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public GameMoveBuilder withPlyIndex(int plyIndex) {
        this.plyIndex = plyIndex;
        return this;
    }

    public GameMoveBuilder withSan(String san) {
        this.san = san;
        return this;
    }

    public GameMoveBuilder withUci(String uci) {
        this.uci = uci;
        return this;
    }

    public GameMoveBuilder withPositionFEN(String positionFEN) {
        this.positionFEN = positionFEN;
        return this;
    }

    public GameMoveBuilder withCommentBefore(String commentBefore) {
        this.commentBefore = commentBefore;
        return this;
    }

    public GameMoveBuilder withCommentAfter(String commentAfter) {
        this.commentAfter = commentAfter;
        return this;
    }

    public GameMoveBuilder withAnalysis(GameMoveAnalysis analysis) {
        this.analysis = analysis;
        return this;
    }

    public GameMove build() {
        return new GameMove(id, plyIndex, san, uci, positionFEN, commentBefore, commentAfter, analysis);
    }
}
