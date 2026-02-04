package ru.chessinsight.testdata;

import ru.chessinsight.infrastructure.persistence.jpa.model.GameAnalysisEntity;
import ru.chessinsight.infrastructure.persistence.jpa.model.GameEntity;
import ru.chessinsight.infrastructure.persistence.jpa.model.GameMoveEntity;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public class GameEntityBuilder {
    private UUID id = UUID.randomUUID();
    private UUID userId = UUID.randomUUID();
    private String event = "Event";
    private String site = "Site";
    private LocalDate date = LocalDate.of(2024, 1, 1);
    private String round = "1";
    private String whiteName = "White";
    private String blackName = "Black";
    private String result = "UNFINISHED";
    private String pgn = "*";
    private GameAnalysisEntity analysis;
    private Set<GameMoveEntity> moves = new TreeSet<>(Comparator.comparing(GameMoveEntity::getPlyIndex));

    public static GameEntityBuilder gameEntity() {
        return new GameEntityBuilder();
    }

    public GameEntityBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public GameEntityBuilder withUserId(UUID userId) {
        this.userId = userId;
        return this;
    }

    public GameEntityBuilder withEvent(String event) {
        this.event = event;
        return this;
    }

    public GameEntityBuilder withSite(String site) {
        this.site = site;
        return this;
    }

    public GameEntityBuilder withDate(LocalDate date) {
        this.date = date;
        return this;
    }

    public GameEntityBuilder withRound(String round) {
        this.round = round;
        return this;
    }

    public GameEntityBuilder withWhiteName(String whiteName) {
        this.whiteName = whiteName;
        return this;
    }

    public GameEntityBuilder withBlackName(String blackName) {
        this.blackName = blackName;
        return this;
    }

    public GameEntityBuilder withResult(String result) {
        this.result = result;
        return this;
    }

    public GameEntityBuilder withPgn(String pgn) {
        this.pgn = pgn;
        return this;
    }

    public GameEntityBuilder withAnalysis(GameAnalysisEntity analysis) {
        this.analysis = analysis;
        return this;
    }

    public GameEntityBuilder withMoves(Set<GameMoveEntity> moves) {
        this.moves = moves;
        return this;
    }

    public GameEntity build() {
        GameEntity entity = new GameEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setEvent(event);
        entity.setSite(site);
        entity.setDate(date);
        entity.setRound(round);
        entity.setWhiteName(whiteName);
        entity.setBlackName(blackName);
        entity.setResult(result);
        entity.setPgn(pgn);
        if (analysis != null) {
            entity.setAnalysis(analysis);
        }
        if (moves != null) {
            entity.setMoves(moves);
        }
        return entity;
    }
}
