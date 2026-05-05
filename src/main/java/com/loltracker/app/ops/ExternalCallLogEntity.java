package com.loltracker.app.ops;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "external_call_logs",
    indexes = {
      @Index(name = "idx_external_call_logs_integration_status", columnList = "integration,status,occurred_at"),
      @Index(name = "idx_external_call_logs_occurred_at", columnList = "occurred_at")
    })
@Getter
@Setter
public class ExternalCallLogEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "integration", nullable = false, length = 24)
  private String integration;

  @Column(name = "operation", nullable = false, length = 64)
  private String operation;

  @Column(name = "status", nullable = false, length = 16)
  private String status;

  @Column(name = "category", length = 32)
  private String category;

  @Column(name = "summary", length = 500)
  private String summary;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  @PrePersist
  void onCreate() {
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }
}
