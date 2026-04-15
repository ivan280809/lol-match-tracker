package com.loltracker.app.match;

import java.time.Instant;

public record TrackedMatchView(
    Long id,
    Long playerId,
    String playerName,
    String matchId,
    String championName,
    String result,
    String gameMode,
    long durationSeconds,
    Instant gameEndAt,
    boolean notificationSent) {

  public static TrackedMatchView fromEntity(TrackedMatchEntity entity) {
    return new TrackedMatchView(
        entity.getId(),
        entity.getPlayer().getId(),
        entity.getPlayer().getGameName() + "#" + entity.getPlayer().getTagLine(),
        entity.getMatchId(),
        entity.getChampionName(),
        entity.getResult(),
        entity.getGameMode(),
        entity.getDurationSeconds(),
        entity.getGameEndAt(),
        entity.isNotificationSent());
  }
}

