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
    String errorSummary) {

  public static PollRunView fromEntity(PollRunEntity entity) {
    return new PollRunView(
        entity.getId(),
        entity.getStartedAt(),
        entity.getFinishedAt(),
        entity.getStatus(),
        entity.getPlayersProcessed(),
        entity.getNewMatchesFound(),
        entity.getNotificationsSent(),
        entity.getErrorSummary());
  }
}

