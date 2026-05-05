package com.loltracker.app.player;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "players",
    uniqueConstraints = @UniqueConstraint(columnNames = {"game_name", "tag_line"}),
    indexes = {
      @Index(name = "idx_players_puuid", columnList = "puuid"),
      @Index(name = "idx_players_active_archived", columnList = "active,archived_at"),
      @Index(name = "idx_players_track_from", columnList = "track_from")
    })
@Getter
@Setter
public class PlayerEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "game_name", nullable = false, length = 64)
  private String gameName;

  @Column(name = "tag_line", nullable = false, length = 32)
  private String tagLine;

  @Enumerated(EnumType.STRING)
  @Column(name = "platform", nullable = false, length = 16, columnDefinition = "varchar(16) default 'EUW1'")
  private RiotPlatform platform = RiotPlatform.defaultPlatform();

  @Column(name = "puuid", length = 128)
  private String puuid;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  @Column(name = "archived_at")
  private Instant archivedAt;

  @Column(name = "last_polled_at")
  private Instant lastPolledAt;

  @Column(name = "track_from")
  private Instant trackFrom;

  @Enumerated(EnumType.STRING)
  @Column(name = "backfill_mode", nullable = false, length = 40)
  private PlayerBackfillMode backfillMode = PlayerBackfillMode.NONE;

  @Column(name = "last_successful_sync_at")
  private Instant lastSuccessfulSyncAt;

  @Column(name = "last_sync_status", length = 32)
  private String lastSyncStatus;

  @Column(name = "last_error", length = 500)
  private String lastError;

  @Column(name = "rank_queue_type", length = 32)
  private String rankQueueType;

  @Column(name = "rank_tier", length = 24)
  private String rankTier;

  @Column(name = "rank_division", length = 8)
  private String rankDivision;

  @Column(name = "rank_league_points")
  private Integer rankLeaguePoints;

  @Column(name = "rank_score")
  private Integer rankScore;

  @Column(name = "rank_updated_at")
  private Instant rankUpdatedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
    if (platform == null) {
      platform = RiotPlatform.defaultPlatform();
    }
    if (lastSyncStatus == null) {
      lastSyncStatus = "NEW";
    }
    if (backfillMode == null) {
      backfillMode = PlayerBackfillMode.NONE;
    }
    if (trackFrom == null) {
      trackFrom = now;
    }
  }

  @PreUpdate
  void onUpdate() {
    if (platform == null) {
      platform = RiotPlatform.defaultPlatform();
    }
    if (backfillMode == null) {
      backfillMode = PlayerBackfillMode.NONE;
    }
    updatedAt = Instant.now();
  }
}

