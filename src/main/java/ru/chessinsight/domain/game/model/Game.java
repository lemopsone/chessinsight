package ru.chessinsight.domain.game.model;

import java.time.LocalDate;
import java.util.UUID;

public class Game {
    private UUID id;
    private UUID userId;

    private String event;
    private String site;
    private LocalDate date;
    private String round;
    private String whiteName;
    private String blackName;
    private GameResult result;

    private String pgn;

    public Game() { }

    public Game(UUID id, UUID userId, String event, String site, LocalDate date, String round, String whiteName, String blackName, GameResult result, String pgn) {
        this.id = id;
        this.userId = userId;
        this.event = event;
        this.site = site;
        this.date = date;
        this.round = round;
        this.whiteName = whiteName;
        this.blackName = blackName;
        this.result = result;
        this.pgn = pgn;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPgn() {
        return pgn;
    }

    public void setPgn(String pgn) {
        this.pgn = pgn;
    }

    public GameResult getResult() {
        return result;
    }

    public void setResult(GameResult result) {
        this.result = result;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getRound() {
        return round;
    }

    public void setRound(String round) {
        this.round = round;
    }

    public String getWhiteName() {
        return whiteName;
    }

    public void setWhiteName(String whiteName) {
        this.whiteName = whiteName;
    }

    public String getBlackName() {
        return blackName;
    }

    public void setBlackName(String blackName) {
        this.blackName = blackName;
    }
}
