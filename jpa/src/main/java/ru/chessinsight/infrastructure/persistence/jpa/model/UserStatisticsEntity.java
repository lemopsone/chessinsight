package ru.chessinsight.infrastructure.persistence.jpa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "user_statistics")
public class UserStatisticsEntity {
    @Id
    @Column(name = "user_id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserEntity user;

    @Column
    private Double accuracy;
    @Column(name = "accuracy_white")
    private Double accuracyWhite;
    @Column(name = "accuracy_black")
    private Double accuracyBlack;

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public Double getAccuracy(){ return accuracy; }
    public void setAccuracy(Double accuracy){ this.accuracy = accuracy; }
    public Double getAccuracyWhite(){ return accuracyWhite; }
    public void setAccuracyWhite(Double v){ this.accuracyWhite = v; }
    public Double getAccuracyBlack(){ return accuracyBlack; }
    public void setAccuracyBlack(Double v){ this.accuracyBlack = v; }
}
