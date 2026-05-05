package com.loltracker.app.match;

import com.loltracker.app.player.PlayerEntity;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "tracked_matches",
    uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "match_id"}),
    indexes = {
      @Index(name = "idx_tracked_matches_player_end", columnList = "player_id,game_end_at"),
      @Index(name = "idx_tracked_matches_match_id", columnList = "match_id")
    })
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

  @Column(name = "champion_id")
  private Integer championId;

  @Column(name = "result", nullable = false, length = 16)
  private String result;

  @Column(name = "game_mode", nullable = false, length = 64)
  private String gameMode;

  @Column(name = "queue_id")
  private Integer queueId;

  @Column(name = "lane", length = 32)
  private String lane;

  @Column(name = "role", length = 32)
  private String role;

  @Column(name = "kills", nullable = false)
  private int kills;

  @Column(name = "deaths", nullable = false)
  private int deaths;

  @Column(name = "assists", nullable = false)
  private int assists;

  @Column(name = "creep_score", nullable = false)
  private int creepScore;

  @Column(name = "gold_earned", nullable = false)
  private int goldEarned;

  @Column(name = "damage_dealt_to_champions", nullable = false)
  private int damageDealtToChampions;

  @Column(name = "vision_score", nullable = false)
  private int visionScore;

  @Column(name = "duration_seconds", nullable = false)
  private long durationSeconds;

  @Column(name = "game_end_at", nullable = false)
  private Instant gameEndAt;

  @Column(name = "platform", length = 32)
  private String platform;

  @Column(name = "region", length = 32)
  private String region;

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

