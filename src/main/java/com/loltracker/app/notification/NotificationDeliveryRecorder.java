package com.loltracker.app.notification;

import com.loltracker.app.match.TrackedMatchEntity;
import com.loltracker.app.match.TrackedMatchRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationDeliveryRecorder {

  private final NotificationOutboxRepository notificationOutboxRepository;
  private final TrackedMatchRepository trackedMatchRepository;
  private final Clock clock;

  @Transactional
  public void recordAttempt(NotificationOutboxEntity outbox) {
    outbox.setAttemptCount(outbox.getAttemptCount() + 1);
    outbox.setLastAttemptAt(now());
    outbox.setLastError(null);
    notificationOutboxRepository.save(outbox);
  }

  @Transactional
  public void recordSent(NotificationOutboxEntity outbox, Integer telegramMessageId) {
    Instant now = now();
    outbox.setStatus(NotificationDeliveryStatus.SENT);
    outbox.setTelegramMessageId(telegramMessageId);
    outbox.setSentAt(now);
    outbox.setNextAttemptAt(now);
    outbox.setLastError(null);
    notificationOutboxRepository.save(outbox);

    TrackedMatchEntity trackedMatch = outbox.getTrackedMatch();
    trackedMatch.setNotificationSent(true);
    trackedMatch.setNotificationSentAt(now);
    trackedMatchRepository.save(trackedMatch);
  }

  @Transactional
  public void recordFailure(NotificationOutboxEntity outbox, RuntimeException exception) {
    outbox.setStatus(NotificationDeliveryStatus.FAILED);
    outbox.setLastError(shortMessage(exception));
    outbox.setNextAttemptAt(now().plus(backoff(outbox.getAttemptCount())));
    notificationOutboxRepository.save(outbox);
  }

  private Duration backoff(int attemptCount) {
    long seconds = Math.min(900, 30L * Math.max(1, attemptCount));
    return Duration.ofSeconds(seconds);
  }

  private String shortMessage(RuntimeException exception) {
    String message = exception.getMessage();
    if (message == null || message.isBlank()) {
      message = exception.getClass().getSimpleName();
    }
    return message.substring(0, Math.min(500, message.length()));
  }

  private Instant now() {
    return clock == null ? Instant.now() : clock.instant();
  }
}
