package com.loltracker.app.player;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "players",
    uniqueConstraints = @UniqueConstraint(columnNames = {"game_name", "tag_line"}))
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

  @Column(name = "puuid", length = 128)
  private String puuid;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  @Column(name = "last_polled_at")
  private Instant lastPolledAt;

  @Column(name = "last_successful_sync_at")
  private Instant lastSuccessfulSyncAt;

  @Column(name = "last_sync_status", length = 32)
  private String lastSyncStatus;

  @Column(name = "last_error", length = 500)
  private String lastError;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
    if (lastSyncStatus == null) {
      lastSyncStatus = "NEW";
    }
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }
}

