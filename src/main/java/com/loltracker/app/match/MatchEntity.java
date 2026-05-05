package com.loltracker.app.match;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "matches",
    uniqueConstraints = @UniqueConstraint(columnNames = "match_id"),
    indexes = {
      @Index(name = "idx_matches_game_end_at", columnList = "game_end_at"),
      @Index(name = "idx_matches_platform_region", columnList = "platform,region")
    })
@Getter
@Setter
public class MatchEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "match_id", nullable = false, length = 64)
  private String matchId;

  @Column(name = "game_mode", nullable = false, length = 64)
  private String gameMode;

  @Column(name = "queue_id")
  private Integer queueId;

  @Column(name = "duration_seconds", nullable = false)
  private long durationSeconds;

  @Column(name = "game_end_at", nullable = false)
  private Instant gameEndAt;

  @Column(name = "platform", length = 32)
  private String platform;

  @Column(name = "region", length = 32)
  private String region;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }
}
