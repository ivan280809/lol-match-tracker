package com.loltracker.app.ops;

import java.time.Instant;

public record ExternalCallLogView(
    Long id,
    String integration,
    String operation,
    String status,
    String category,
    String summary,
    Instant occurredAt) {

  public static ExternalCallLogView fromEntity(ExternalCallLogEntity entity) {
    return new ExternalCallLogView(
        entity.getId(),
        entity.getIntegration(),
        entity.getOperation(),
        entity.getStatus(),
        entity.getCategory(),
        entity.getSummary(),
        entity.getOccurredAt());
  }
}
