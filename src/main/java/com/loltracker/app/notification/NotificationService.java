package com.loltracker.app.notification;

import com.loltracker.app.integration.telegram.TelegramDeliveryReceipt;
import com.loltracker.app.integration.telegram.TelegramNotifier;
import com.loltracker.app.match.TrackedMatchEntity;
import com.loltracker.app.player.PlayerEntity;
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

  private final TelegramNotifier telegramNotifier;
  private final NotificationMessageFactory notificationMessageFactory;
  private final NotificationStatsService notificationStatsService;
  private final NotificationOutboxRepository notificationOutboxRepository;
  private final NotificationDeliveryRecorder notificationDeliveryRecorder;

  @Transactional
  public NotificationOutboxEntity enqueueMatchNotification(TrackedMatchEntity trackedMatch) {
    return notificationOutboxRepository
        .findByTrackedMatchId(trackedMatch.getId())
        .orElseGet(() -> createOutbox(trackedMatch));
  }

  public NotificationDispatchResult dispatchPendingForPlayer(PlayerEntity player) {
    List<NotificationOutboxEntity> outboxItems =
        notificationOutboxRepository
            .findTop50ByTrackedMatchPlayerIdAndStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                player.getId(), RETRYABLE_STATUSES, Instant.now());
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
    entity.setNextAttemptAt(Instant.now().minusMillis(1));
    try {
      return notificationOutboxRepository.save(entity);
    } catch (DataIntegrityViolationException e) {
      return notificationOutboxRepository.findByTrackedMatchId(trackedMatch.getId()).orElseThrow(() -> e);
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
      return new NotificationDispatchResult(1, 0);
    } catch (RuntimeException e) {
      log.warn(
          "Notification delivery failed for outbox {} and match {}",
          outbox.getId(),
          outbox.getTrackedMatch().getMatchId(),
          e);
      notificationDeliveryRecorder.recordFailure(outbox, e);
      return new NotificationDispatchResult(0, 1);
    }
  }
}

