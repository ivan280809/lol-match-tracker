package com.loltracker.app.match;

import com.loltracker.app.player.PlayerEntity;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "tracked_matches",
    uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "match_id"}))
@Getter
@Setter
public class TrackedMatchEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "player_id", nullable = false)
  private PlayerEntity player;

  @Column(name = "match_id", nullable = false, length = 64)
  private String matchId;

  @Column(name = "champion_name", nullable = false, length = 64)
  private String championName;

  @Column(name = "result", nullable = false, length = 16)
  private String result;

  @Column(name = "game_mode", nullable = false, length = 64)
  private String gameMode;

  @Column(name = "duration_seconds", nullable = false)
  private long durationSeconds;

  @Column(name = "game_end_at", nullable = false)
  private Instant gameEndAt;

  @Column(name = "notification_sent", nullable = false)
  private boolean notificationSent;

  @Column(name = "notification_sent_at")
  private Instant notificationSentAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
  }
}

