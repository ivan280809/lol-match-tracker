package com.loltracker.app.player;

import java.time.Instant;

public record PlayerView(
    Long id,
    RiotPlatform platform,
    String gameName,
    String tagLine,
    String puuid,
    boolean active,
    Instant lastPolledAt,
    Instant lastSuccessfulSyncAt,
    String lastSyncStatus,
    String lastError) {

  public PlayerView(
      Long id,
      String gameName,
      String tagLine,
      String puuid,
      boolean active,
      Instant lastPolledAt,
      Instant lastSuccessfulSyncAt,
      String lastSyncStatus,
      String lastError) {
    this(
        id,
        RiotPlatform.defaultPlatform(),
        gameName,
        tagLine,
        puuid,
        active,
        lastPolledAt,
        lastSuccessfulSyncAt,
        lastSyncStatus,
        lastError);
  }

  public static PlayerView fromEntity(PlayerEntity player) {
    return new PlayerView(
        player.getId(),
        RiotPlatform.fromFormValue(player.getPlatform()),
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

