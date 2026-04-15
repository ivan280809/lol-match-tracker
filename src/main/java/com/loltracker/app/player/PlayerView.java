package com.loltracker.app.player;

import java.time.Instant;

public record PlayerView(
    Long id,
    String gameName,
    String tagLine,
    String puuid,
    boolean active,
    Instant lastPolledAt,
    Instant lastSuccessfulSyncAt,
    String lastSyncStatus,
    String lastError) {

  public static PlayerView fromEntity(PlayerEntity player) {
    return new PlayerView(
        player.getId(),
        player.getGameName(),
        player.getTagLine(),
        player.getPuuid(),
        player.isActive(),
        player.getLastPolledAt(),
        player.getLastSuccessfulSyncAt(),
        player.getLastSyncStatus(),
        player.getLastError());
  }
}

