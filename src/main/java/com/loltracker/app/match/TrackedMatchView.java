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
    Integer queueId,
    String queueLabel,
    MatchQueueType queueType,
    long durationSeconds,
    Instant gameEndAt,
    boolean notificationSent) {

  public static TrackedMatchView fromEntity(TrackedMatchEntity entity) {
    MatchQueueDescriptor queue = MatchQueueCatalog.describe(entity.getQueueId(), entity.getGameMode());
    return new TrackedMatchView(
        entity.getId(),
        entity.getPlayer().getId(),
        entity.getPlayer().getGameName() + "#" + entity.getPlayer().getTagLine(),
        entity.getMatchId(),
        entity.getChampionName(),
        MatchResult.fromStoredValue(entity.getResult()).name(),
        entity.getGameMode(),
        queue.queueId(),
        queue.label(),
        queue.type(),
        entity.getDurationSeconds(),
        entity.getGameEndAt(),
        entity.isNotificationSent());
  }

  public static TrackedMatchView fromPlayerMatch(PlayerMatchEntity entity) {
    MatchEntity match = entity.getMatch();
    MatchQueueDescriptor queue = MatchQueueCatalog.describe(match.getQueueId(), match.getGameMode());
    boolean notificationHandled = entity.isNotificationSuppressed();
    return new TrackedMatchView(
        entity.getId(),
        entity.getPlayer().getId(),
        entity.getPlayer().getGameName() + "#" + entity.getPlayer().getTagLine(),
        match.getMatchId(),
        entity.getChampionName(),
        entity.getResult().name(),
        match.getGameMode(),
        queue.queueId(),
        queue.label(),
        queue.type(),
        match.getDurationSeconds(),
        match.getGameEndAt(),
        notificationHandled);
  }

  public String notificationLabel() {
    return notificationSent ? "Aviso resuelto" : "Aviso gestionado por outbox";
  }
}

