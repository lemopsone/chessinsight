package ru.chessinsight.infrastructure.persistence.jpa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "game_analysis")
public class GameAnalysisEntity {
    @Id
    @Column(name = "game_id")
    private UUID id;
    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private GameEntity game;

    @Column(name = "accuracy_white")
    private Double accuracyWhite;

    @Column(name = "accuracy_black")
    private Double accuracyBlack;

    @Column
    private Integer inaccuracies;
    @Column
    private Integer mistakes;
    @Column
    private Integer blunders;

    @Column(name = "analyzed_at")
    private OffsetDateTime analyzedAt;

    public GameEntity getGame(){ return game; }
    public void setGame(GameEntity game){ this.game = game; }
    public Double getAccuracyWhite(){ return accuracyWhite; }
    public void setAccuracyWhite(Double v){ this.accuracyWhite = v; }
    public Double getAccuracyBlack(){ return accuracyBlack; }
    public void setAccuracyBlack(Double v){ this.accuracyBlack = v; }
    public Integer getInaccuracies(){ return inaccuracies; }
    public void setInaccuracies(Integer v){ this.inaccuracies = v; }
    public Integer getMistakes(){ return mistakes; }
    public void setMistakes(Integer v){ this.mistakes = v; }
    public Integer getBlunders(){ return blunders; }
    public void setBlunders(Integer v){ this.blunders = v; }
    public OffsetDateTime getAnalyzedAt(){ return analyzedAt; }
    public void setAnalyzedAt(OffsetDateTime v){ this.analyzedAt = v; }
}
