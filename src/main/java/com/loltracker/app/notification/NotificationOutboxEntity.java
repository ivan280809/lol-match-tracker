package com.loltracker.app.notification;

import com.loltracker.app.match.TrackedMatchEntity;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "notification_outbox",
    uniqueConstraints = @UniqueConstraint(columnNames = "tracked_match_id"),
    indexes = {
      @Index(name = "idx_notification_outbox_status_next_attempt", columnList = "status,next_attempt_at"),
      @Index(name = "idx_notification_outbox_created_at", columnList = "created_at")
    })
@Getter
@Setter
public class NotificationOutboxEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tracked_match_id", nullable = false)
  private TrackedMatchEntity trackedMatch;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private NotificationDeliveryStatus status = NotificationDeliveryStatus.PENDING;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount;

  @Column(name = "last_attempt_at")
  private Instant lastAttemptAt;

  @Column(name = "next_attempt_at", nullable = false)
  private Instant nextAttemptAt;

  @Column(name = "last_error", length = 500)
  private String lastError;

  @Column(name = "telegram_message_id")
  private Integer telegramMessageId;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
    if (nextAttemptAt == null) {
      nextAttemptAt = now;
    }
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }
}
