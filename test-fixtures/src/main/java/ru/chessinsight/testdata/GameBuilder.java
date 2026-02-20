package ru.chessinsight.testdata;

import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.model.GameAnalysis;
import ru.chessinsight.domain.game.model.GameMove;
import ru.chessinsight.domain.game.model.GameResult;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public class GameBuilder {
    private UUID id = UUID.randomUUID();
    private UUID userId = UUID.randomUUID();
    private String event = "Event";
    private String site = "Site";
    private LocalDate date = LocalDate.of(2024, 1, 1);
    private String round = "1";
    private String whiteName = "White";
    private String blackName = "Black";
    private GameResult result = GameResult.UNFINISHED;
    private String pgn = "*";
    private GameAnalysis analysis;
    private Set<GameMove> moves = new TreeSet<>(Comparator.comparing(GameMove::getPlyIndex));

    public static GameBuilder game() {
        return new GameBuilder();
    }

    public GameBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public GameBuilder withUserId(UUID userId) {
        this.userId = userId;
        return this;
    }

    public GameBuilder withEvent(String event) {
        this.event = event;
        return this;
    }

    public GameBuilder withSite(String site) {
        this.site = site;
        return this;
    }

    public GameBuilder withDate(LocalDate date) {
        this.date = date;
        return this;
    }

    public GameBuilder withRound(String round) {
        this.round = round;
        return this;
    }

    public GameBuilder withWhiteName(String whiteName) {
        this.whiteName = whiteName;
        return this;
    }

    public GameBuilder withBlackName(String blackName) {
        this.blackName = blackName;
        return this;
    }

    public GameBuilder withResult(GameResult result) {
        this.result = result;
        return this;
    }

    public GameBuilder withPgn(String pgn) {
        this.pgn = pgn;
        return this;
    }

    public GameBuilder withAnalysis(GameAnalysis analysis) {
        this.analysis = analysis;
        return this;
    }

    public GameBuilder withMoves(Set<GameMove> moves) {
        this.moves = moves;
        return this;
    }

    public Game build() {
        return new Game(id, userId, event, site, date, round, whiteName, blackName, result, pgn, analysis, moves);
    }
}
