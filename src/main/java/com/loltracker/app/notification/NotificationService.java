package com.loltracker.app.notification;

import com.loltracker.app.integration.telegram.TelegramDeliveryReceipt;
import com.loltracker.app.integration.telegram.TelegramNotificationPort;
import com.loltracker.app.match.TrackedMatchEntity;
import com.loltracker.app.ops.OpsMetrics;
import com.loltracker.app.player.PlayerEntity;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    List<NotificationOutboxEntity> outboxItems =
        notificationOutboxRepository
            .findTop50ByTrackedMatchPlayerIdAndStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                player.getId(), RETRYABLE_STATUSES, now());
    NotificationDispatchResult result = NotificationDispatchResult.empty();
    for (NotificationOutboxEntity outbox : outboxItems) {
      result = result.plus(dispatch(outbox));
    }
    return result;
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

  private NotificationDispatchResult dispatch(NotificationOutboxEntity outbox) {
    notificationDeliveryRecorder.recordAttempt(outbox);
    try {
      TrackedMatchEntity trackedMatch = outbox.getTrackedMatch();
      NotificationStatsSnapshot stats = notificationStatsService.buildFor(trackedMatch);
      TelegramDeliveryReceipt receipt =
          telegramNotifier.send(notificationMessageFactory.build(trackedMatch, stats));
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
      return new NotificationDispatchResult(0, 1);
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

