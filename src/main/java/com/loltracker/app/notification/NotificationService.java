package com.loltracker.app.notification;

import com.loltracker.app.integration.telegram.TelegramDeliveryReceipt;
import com.loltracker.app.integration.telegram.TelegramNotificationPort;
import com.loltracker.app.match.TrackedMatchEntity;
import com.loltracker.app.ops.OpsMetrics;
import com.loltracker.app.player.PlayerEntity;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import com.loltracker.app.notification.NotificationDeliveryStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

  private static final List<NotificationDeliveryStatus> RETRYABLE_STATUSES =
      List.of(NotificationDeliveryStatus.PENDING, NotificationDeliveryStatus.FAILED);

  private final TelegramNotificationPort telegramNotifier;
  private final NotificationMessageFactory notificationMessageFactory;
  private final NotificationStatsService notificationStatsService;
  private final NotificationOutboxRepository notificationOutboxRepository;
  private final NotificationDeliveryRecorder notificationDeliveryRecorder;
  private final Clock clock;
  private final OpsMetrics opsMetrics;

  @Transactional
  public NotificationOutboxEntity enqueueMatchNotification(TrackedMatchEntity trackedMatch) {
    return notificationOutboxRepository.findByTrackedMatchId(trackedMatch.getId())
        .map(
            existing -> {
              recordOutboxEnqueue("existing");
              return existing;
            })
        .orElseGet(() -> createOutbox(trackedMatch));
  }

  public NotificationDispatchResult dispatchPendingForPlayer(PlayerEntity player) {
    return dispatchPendingForPlayer(player, 50);
  }

  public NotificationDispatchResult dispatchPendingForPlayer(PlayerEntity player, int maximumMessages) {
    return dispatchPendingForPlayer(player, maximumMessages, Long.MAX_VALUE);
  }

  public NotificationDispatchResult dispatchPendingForPlayer(
      PlayerEntity player, int maximumMessages, long deadlineNanos) {
    return dispatchPendingForPlayer(player, maximumMessages, deadlineNanos, 0L);
  }

  public NotificationDispatchResult dispatchPendingForPlayer(
      PlayerEntity player, int maximumMessages, long deadlineNanos, long minimumSendWindowNanos) {
    if (maximumMessages <= 0) {
      return NotificationDispatchResult.empty();
    }
    if (activeTelegramRateLimitUntil() != null) {
      return NotificationDispatchResult.empty();
    }
    List<NotificationOutboxEntity> outboxItems =
        notificationOutboxRepository
            .findTop50ByTrackedMatchPlayerIdAndStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                player.getId(), RETRYABLE_STATUSES, now());
    NotificationDispatchResult result = NotificationDispatchResult.empty();
    for (NotificationOutboxEntity outbox : outboxItems.stream().limit(maximumMessages).toList()) {
      if (deadlineNanos - System.nanoTime() < minimumSendWindowNanos) {
        break;
      }
      NotificationDispatchResult delivery = dispatch(outbox, deadlineNanos);
      result = result.plus(delivery);
      if (delivery.rateLimited()) {
        break;
      }
    }
    return result;
  }

  @Transactional(readOnly = true)
  public Instant activeTelegramRateLimitUntil() {
    return notificationOutboxRepository
        .findTopByStatusAndLastErrorStartingWithOrderByNextAttemptAtDesc(
            NotificationDeliveryStatus.FAILED, NotificationDeliveryRecorder.TELEGRAM_RATE_LIMIT_ERROR_PREFIX)
        .map(NotificationOutboxEntity::getNextAttemptAt)
        .filter(until -> until.isAfter(now()))
        .orElse(null);
  }

  @Transactional(readOnly = true)
  public long countPendingNotifications() {
    return notificationOutboxRepository.countByStatus(NotificationDeliveryStatus.PENDING);
  }

  @Transactional(readOnly = true)
  public long countFailedNotifications() {
    return notificationOutboxRepository.countByStatus(NotificationDeliveryStatus.FAILED);
  }

  @Transactional(readOnly = true)
  public List<NotificationOutboxView> getProblemNotificationsForPlayer(Long playerId) {
    return notificationOutboxRepository
        .findTop20ByTrackedMatchPlayerIdAndStatusInOrderByCreatedAtDesc(playerId, RETRYABLE_STATUSES)
        .stream()
        .map(NotificationOutboxView::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<NotificationOutboxView> getRecentProblemNotifications() {
    return notificationOutboxRepository.findTop20ByStatusInOrderByCreatedAtDesc(RETRYABLE_STATUSES).stream()
        .map(NotificationOutboxView::fromEntity)
        .toList();
  }

  private NotificationOutboxEntity createOutbox(TrackedMatchEntity trackedMatch) {
    NotificationOutboxEntity entity = new NotificationOutboxEntity();
    entity.setTrackedMatch(trackedMatch);
    entity.setStatus(NotificationDeliveryStatus.PENDING);
    entity.setNextAttemptAt(now().minusMillis(1));
    try {
      NotificationOutboxEntity saved = notificationOutboxRepository.save(entity);
      recordOutboxEnqueue("created");
      return saved;
    } catch (DataIntegrityViolationException e) {
      NotificationOutboxEntity existing =
          notificationOutboxRepository.findByTrackedMatchId(trackedMatch.getId()).orElseThrow(() -> e);
      recordOutboxEnqueue("existing");
      return existing;
    }
  }

  private NotificationDispatchResult dispatch(NotificationOutboxEntity outbox, long deadlineNanos) {
    notificationDeliveryRecorder.recordAttempt(outbox);
    // Abort early if the deadline has already elapsed.  This check is done
    // immediately after the attempt has been recorded, so that the caller
    // can decide whether to keep the item pending or mark it failed.
    if (deadlineNanos != Long.MAX_VALUE && deadlineNanos <= System.nanoTime()) {
      log.debug("Skipping Telegram delivery for outbox {} due to expired deadline", outbox.getId());
      return NotificationDispatchResult.empty();
    }
    try {
      // Determine the remaining time budget before any expensive operations.
      long remainingNanos = deadlineNanos == Long.MAX_VALUE
          ? Long.MAX_VALUE
          : deadlineNanos - System.nanoTime();
      if (remainingNanos <= 0L) {
        log.debug("Skipping Telegram delivery for outbox {} due to no remaining time", outbox.getId());
        return NotificationDispatchResult.empty();
      }
      TrackedMatchEntity trackedMatch = outbox.getTrackedMatch();
      NotificationStatsSnapshot stats = notificationStatsService.buildFor(trackedMatch);
      String message = notificationMessageFactory.build(trackedMatch, stats);

      // Re‑check the deadline just before invoking Telegram. If the deadline
      // has been exceeded during the expensive `buildFor` or `build` calls
      // (which may consume a non‑trivial amount of time), skip the send
      // entirely so we don't trigger a 1‑nanosecond timeout.
      if (deadlineNanos != Long.MAX_VALUE) {
        long remainingBeforeSend = deadlineNanos - System.nanoTime();
        if (remainingBeforeSend <= 0L) {
          log.debug("Skipping Telegram delivery for outbox {} due to deadline exceeded after preparation", outbox.getId());
          return NotificationDispatchResult.empty();
        }
        long timeoutNanos = Math.max(1L, remainingBeforeSend);
        TelegramDeliveryReceipt receipt = telegramNotifier.send(message, java.time.Duration.ofNanos(timeoutNanos));
        Integer telegramMessageId = receipt == null ? null : receipt.messageId();
        notificationDeliveryRecorder.recordSent(outbox, telegramMessageId);
        recordOutboxDispatch("sent");
        return new NotificationDispatchResult(1, 0);
      }

      TelegramDeliveryReceipt receipt = telegramNotifier.send(message);
      Integer telegramMessageId = receipt == null ? null : receipt.messageId();
      notificationDeliveryRecorder.recordSent(outbox, telegramMessageId);
      recordOutboxDispatch("sent");
      return new NotificationDispatchResult(1, 0);
    } catch (RuntimeException e) {
      log.warn(
          "Notification delivery failed for outbox {} and match {}",
          outbox.getId(),
          outbox.getTrackedMatch().getMatchId(),
          e);
      notificationDeliveryRecorder.recordFailure(outbox, e);
      recordOutboxDispatch("failed");
      boolean rateLimited = e instanceof RestClientResponseException response
          && response.getStatusCode().value() == 429;
      return new NotificationDispatchResult(0, 1, rateLimited);
    }
  }

  private Instant now() {
    return clock == null ? Instant.now() : clock.instant();
  }

  private void recordOutboxEnqueue(String result) {
    if (opsMetrics != null) {
      opsMetrics.recordOutboxEnqueue(result);
    }
  }

  private void recordOutboxDispatch(String result) {
    if (opsMetrics != null) {
      opsMetrics.recordOutboxDispatch(result);
    }
  }
}
