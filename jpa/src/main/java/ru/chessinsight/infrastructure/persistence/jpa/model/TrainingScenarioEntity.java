package ru.chessinsight.infrastructure.persistence.jpa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "training_scenario")
public class TrainingScenarioEntity {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "game_id")
    private UUID gameId;

    @Column(name = "position_fen")
    private String positionFEN;

    @Column(name = "pv_san")
    private String pvSan;

    @Column(name = "pv_uci")
    private String pvUci;

    @Column
    private String prompt;

    @Column
    private boolean completed;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    public UUID getId(){ return id; }
    public void setId(UUID id){ this.id = id; }
    public UUID getUserId(){ return userId; }
    public void setUserId(UUID userId){ this.userId = userId; }
    public UUID getGameId(){ return gameId; }
    public void setGameId(UUID gameId){ this.gameId = gameId; }
    public String getPositionFEN(){ return positionFEN; }
    public void setPositionFEN(String positionFEN){ this.positionFEN = positionFEN; }
    public String getPvSan(){ return pvSan; }
    public void setPvSan(String pvSan){ this.pvSan = pvSan; }
    public String getPvUci(){ return pvUci; }
    public void setPvUci(String pvUci){ this.pvUci = pvUci; }
    public String getPrompt(){ return prompt; }
    public void setPrompt(String prompt){ this.prompt = prompt; }
    public boolean isCompleted(){ return completed; }
    public void setCompleted(boolean completed){ this.completed = completed; }
    public OffsetDateTime getCompletedAt(){ return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt){ this.completedAt = completedAt; }
}
