package com.loltracker.app.ops;

import java.time.Instant;

public record PollRunView(
    Long id,
    Instant startedAt,
    Instant finishedAt,
    String status,
    int playersProcessed,
    int newMatchesFound,
    int notificationsSent,
    String errorSummary,
    String currentPlayer,
    String currentStage,
    Instant rateLimitPausedUntil,
    String rateLimitMessage) {

  public PollRunView(
      Long id,
      Instant startedAt,
      Instant finishedAt,
      String status,
      int playersProcessed,
      int newMatchesFound,
      int notificationsSent,
      String errorSummary) {
    this(id, startedAt, finishedAt, status, playersProcessed, newMatchesFound, notificationsSent, errorSummary, null, null, null, null);
  }

  public static PollRunView fromEntity(PollRunEntity entity) {
    return new PollRunView(
        entity.getId(),
        entity.getStartedAt(),
        entity.getFinishedAt(),
        entity.getStatus(),
        entity.getPlayersProcessed(),
        entity.getNewMatchesFound(),
        entity.getNotificationsSent(),
        entity.getErrorSummary(),
        entity.getCurrentPlayer(),
        entity.getCurrentStage(),
        entity.getRateLimitPausedUntil(),
        entity.getRateLimitMessage());
  }

  public boolean active() {
    return "RUNNING".equals(status);
  }

  public boolean rateLimited() {
    return rateLimitPausedUntil != null;
  }

  public long durationSeconds() {
    if (startedAt == null) {
      return 0;
    }
    Instant end = finishedAt == null ? Instant.now() : finishedAt;
    return Math.max(0, java.time.Duration.between(startedAt, end).toSeconds());
  }
}

