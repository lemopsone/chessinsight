package ru.chessinsight.infrastructure.persistence.jpa.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.*;

@Entity
@Table(name = "game")
public class GameEntity {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(columnDefinition = "text")
    private String event;
    @Column(columnDefinition = "text")
    private String site;
    @Column
    private LocalDate date;
    @Column(columnDefinition = "text")
    private String round;
    @Column(name = "white_name", columnDefinition = "text")
    private String whiteName;
    @Column(name = "black_name", columnDefinition = "text")
    private String blackName;
    @Column(columnDefinition = "text")
    private String result;
    @Column(columnDefinition = "text")
    private String pgn;

    @OneToOne(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private GameAnalysisEntity analysis;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private final Set<GameMoveEntity> moves = new TreeSet<>(Comparator.comparing(GameMoveEntity::getPlyIndex));

    public UUID getId(){ return id; }
    public void setId(UUID id){ this.id = id; }
    public UUID getUserId(){ return userId; }
    public void setUserId(UUID userId){ this.userId = userId; }
    public String getEvent(){ return event; }
    public void setEvent(String event){ this.event = event; }
    public String getSite(){ return site; }
    public void setSite(String site){ this.site = site; }
    public LocalDate getDate(){ return date; }
    public void setDate(LocalDate date){ this.date = date; }
    public String getRound(){ return round; }
    public void setRound(String round){ this.round = round; }
    public String getWhiteName(){ return whiteName; }
    public void setWhiteName(String whiteName){ this.whiteName = whiteName; }
    public String getBlackName(){ return blackName; }
    public void setBlackName(String blackName){ this.blackName = blackName; }
    public String getResult(){ return result; }
    public void setResult(String result){ this.result = result; }
    public String getPgn(){ return pgn; }
    public void setPgn(String pgn){ this.pgn = pgn; }
    public GameAnalysisEntity getAnalysis(){ return analysis; }
    public void setAnalysis(GameAnalysisEntity analysis){
        this.analysis = analysis;
        if (analysis != null) analysis.setGame(this);
    }

    public Set<GameMoveEntity> getMoves() {
        return moves;
    }

    public void addMove(GameMoveEntity move) {
        if (move != null) {
            move.setGame(this);
            moves.add(move);
        }
    }

    public void removeMove(GameMoveEntity move) {
        if (move != null) {
            moves.remove(move);
            move.setGame(null);
        }
    }

    public void setMoves(Set<GameMoveEntity> moves) {
        this.moves.clear();
        for (var move : moves) {
            this.addMove(move);
        }
    }
}
