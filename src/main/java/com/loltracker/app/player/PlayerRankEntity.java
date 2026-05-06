package com.loltracker.app.player;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
    name = "player_rank_snapshots",
    uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "queue_type"}),
    indexes = {
      @Index(name = "idx_player_rank_snapshots_queue_score", columnList = "queue_type,score")
    })
@Getter
@Setter
public class PlayerRankEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "player_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private PlayerEntity player;

  @Column(name = "queue_type", nullable = false, length = 32)
  private String queueType;

  @Column(name = "tier", nullable = false, length = 24)
  private String tier;

  @Column(name = "division", nullable = false, length = 8)
  private String division;

  @Column(name = "league_points", nullable = false)
  private int leaguePoints;

  @Column(name = "score", nullable = false)
  private int score;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  @PreUpdate
  void touch() {
    if (updatedAt == null) {
      updatedAt = Instant.now();
    }
  }
}
