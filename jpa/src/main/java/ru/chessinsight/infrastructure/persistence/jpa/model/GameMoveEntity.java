package ru.chessinsight.infrastructure.persistence.jpa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "game_move")
public class GameMoveEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(targetEntity = GameEntity.class, fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id")
    private GameEntity game;

    @Column(name = "ply_index")
    private Integer plyIndex;

    @Column(columnDefinition = "text")
    private String san;

    @Column(columnDefinition = "text")
    private String uci;

    @Column(name = "position_fen", columnDefinition = "text")
    private String positionFEN;

    @Column(name = "comment_before", columnDefinition = "text")
    private String commentBefore;

    @Column(name = "comment_after", columnDefinition = "text")
    private String commentAfter;

    @Column(name = "analysis_eval_cp")
    private Double analysisEvalCp;

    @Column(name = "analysis_mate_score")
    private Integer analysisMateScore;

    @Column(name = "analysis_best_uci", columnDefinition = "text")
    private String analysisBestUci;

    @Column(name = "analysis_cp_loss")
    private Double analysisCpLoss;

    @Column(name = "analysis_category")
    private String analysisCategory;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Integer getPlyIndex(){ return plyIndex; }
    public void setPlyIndex(Integer plyIndex){ this.plyIndex = plyIndex; }
    public String getSan(){ return san; }
    public void setSan(String san){ this.san = san; }
    public String getUci(){ return uci; }
    public void setUci(String uci){ this.uci = uci; }
    public String getPositionFEN(){ return positionFEN; }
    public void setPositionFEN(String positionFEN){ this.positionFEN = positionFEN; }
    public String getCommentBefore(){ return commentBefore; }
    public void setCommentBefore(String commentBefore){ this.commentBefore = commentBefore; }
    public String getCommentAfter(){ return commentAfter; }
    public void setCommentAfter(String commentAfter){ this.commentAfter = commentAfter; }
    public Double getAnalysisEvalCp(){ return analysisEvalCp; }
    public void setAnalysisEvalCp(Double v){ this.analysisEvalCp = v; }
    public Integer getAnalysisMateScore(){ return analysisMateScore; }
    public void setAnalysisMateScore(Integer v){ this.analysisMateScore = v; }
    public String getAnalysisBestUci(){ return analysisBestUci; }
    public void setAnalysisBestUci(String v){ this.analysisBestUci = v; }
    public Double getAnalysisCpLoss(){ return analysisCpLoss; }
    public void setAnalysisCpLoss(Double v){ this.analysisCpLoss = v; }
    public String getAnalysisCategory(){ return analysisCategory; }
    public void setAnalysisCategory(String v){ this.analysisCategory = v; }

    public GameEntity getGame() {
        return game;
    }

    public void setGame(GameEntity game) {
        this.game = game;
    }
}
