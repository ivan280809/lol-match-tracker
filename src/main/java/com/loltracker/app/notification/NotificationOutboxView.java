package com.loltracker.app.notification;

import java.time.Instant;

public record NotificationOutboxView(
    Long id,
    Long playerId,
    String playerName,
    String matchId,
    String championName,
    String status,
    int attemptCount,
    Instant lastAttemptAt,
    Instant nextAttemptAt,
    Instant sentAt,
    String lastError) {

  public static NotificationOutboxView fromEntity(NotificationOutboxEntity entity) {
    return new NotificationOutboxView(
        entity.getId(),
        entity.getTrackedMatch().getPlayer().getId(),
        entity.getTrackedMatch().getPlayer().getGameName()
            + "#"
            + entity.getTrackedMatch().getPlayer().getTagLine(),
        entity.getTrackedMatch().getMatchId(),
        entity.getTrackedMatch().getChampionName(),
        entity.getStatus().name(),
        entity.getAttemptCount(),
        entity.getLastAttemptAt(),
        entity.getNextAttemptAt(),
        entity.getSentAt(),
        entity.getLastError());
  }
}
