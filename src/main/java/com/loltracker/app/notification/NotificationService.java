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
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import com.loltracker.app.player.PlayerRankSnapshot;
import com.loltracker.app.player.PlayerRankService;
import com.loltracker.app.player.PlayerRankService.RankRefreshResult;
import com.loltracker.app.notification.NotificationDeliveryStatus;
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
  private final PlayerRankService playerRankService;

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

  /**
   * Dispatch up to 50 pending notifications for the given player. To reduce the number of
   * database and external calls, rank information and roster averages are cached per batch.
   */
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
    Map<RankCacheKey, RankRefreshResult> rankCache = new HashMap<>();
    Set<RankCacheKey> refreshedKeys = new HashSet<>();

    // Compute roster average once per batch instead of per notification.
    Optional<PlayerRankSnapshot> rosterAvg =
        playerRankService == null
            ? Optional.empty()
            : playerRankService.rosterAverageRank();

    for (NotificationOutboxEntity outbox : outboxItems.stream().limit(maximumMessages).toList()) {
      if (deadlineNanos - System.nanoTime() < minimumSendWindowNanos) {
        break;
      }
      NotificationDispatchResult delivery = dispatch(outbox, deadlineNanos, rankCache, rosterAvg, refreshedKeys);
      result = result.plus(delivery);
      if (delivery.rateLimited()) {
        break;
      }
    }
    return result;
  }

  /**
   * Dispatch a single outbox item using the provided cached rank and roster values.
   */
  private NotificationDispatchResult dispatch(
      NotificationOutboxEntity outbox,
      long deadlineNanos,
      Map<RankCacheKey, RankRefreshResult> rankCache,
      Optional<PlayerRankSnapshot> cachedRosterAvg,
      Set<RankCacheKey> refreshedKeys) {
    try {
      notificationDeliveryRecorder.recordAttempt(outbox);
      if (deadlineNanos != Long.MAX_VALUE && deadlineNanos <= System.nanoTime()) {
        return NotificationDispatchResult.empty();
      }
      TrackedMatchEntity trackedMatch = outbox.getTrackedMatch();
      Long playerId = trackedMatch.getPlayer().getId();
      NotificationStatsService.RankQueueContext rankQueue =
          NotificationStatsService.rankQueueContext(trackedMatch);
      RankCacheKey cacheKey = new RankCacheKey(playerId, rankQueue.queueType());
      RankRefreshResult cachedRank =
          playerRankService == null
              ? RankRefreshResult.unranked()
              : rankCache.computeIfAbsent(
                  cacheKey,
                  key -> {
                    RankRefreshResult rr =
                        playerRankService.rankForNotification(
                            trackedMatch.getPlayer(),
                            trackedMatch.getPlayer().getPuuid(),
                            key.queueType());
                    if (rr.refreshed()) {
                      refreshedKeys.add(key);
                    }
                    return rr;
                  });
      // Invalidate other queue entries only if this key was freshly refreshed **and** the
      // queue type is defined. When the queue type is null (representing the "overall"
      // rank), we keep the cached value because a refresh of a specific queue should not
      // discard the stored overall rank.
      // When a specific queue is refreshed we need to invalidate the cached
      // overall rank (null queueType) so that subsequent requests for the
      // overall rank use the newly stored snapshot. We also remove any
      // stale cache entries for other specific queues for the same player.
      if (refreshedKeys.remove(cacheKey) && cacheKey.queueType() != null) {
        // Remove the overall rank cache for this player.
        rankCache.remove(new RankCacheKey(cacheKey.playerId(), null));
        // Remove any other specific‑queue entries that are now stale.
        rankCache.keySet()
            .removeIf(
                k -> k.playerId().equals(cacheKey.playerId())
                    && k.queueType() != null
                    && !k.queueType().equals(cacheKey.queueType()));
      }
      // Build statistics for the match.  The test suite stubs the original
      // overload, so we continue to use it to keep compatibility.
      // Use cached rank and roster values when building the stats snapshot.
      // The NotificationStatsService has an overloaded buildFor that accepts the
      // pre‑computed rank and roster information.  Using this overload avoids
      // additional database and Riot API queries for every notification in a
      // batch.
      NotificationStatsSnapshot stats =
          notificationStatsService.buildFor(trackedMatch, cachedRank, cachedRosterAvg);
      String message = notificationMessageFactory.build(trackedMatch, stats);
      TelegramDeliveryReceipt receipt;
      if (deadlineNanos != Long.MAX_VALUE) {
        long remainingBeforeSend = deadlineNanos - System.nanoTime();
        if (remainingBeforeSend <= 0L) {
          return NotificationDispatchResult.empty();
        }
        receipt = telegramNotifier.send(message, Duration.ofNanos(remainingBeforeSend));
      } else {
        receipt = telegramNotifier.send(message);
      }
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
      boolean rateLimited = e instanceof org.springframework.web.client.RestClientResponseException response
          && response.getStatusCode().value() == 429;
      return new NotificationDispatchResult(0, 1, rateLimited);
    }
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
        .findTop20ByTrackedMatchPlayerIdAndStatusInOrderByCreatedAtDesc(
            playerId, RETRYABLE_STATUSES)
        .stream()
        .map(NotificationOutboxView::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<NotificationOutboxView> getRecentProblemNotifications() {
    return notificationOutboxRepository
        .findTop20ByStatusInOrderByCreatedAtDesc(RETRYABLE_STATUSES)
        .stream()
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

// The legacy single‑parameter dispatch method was removed in the refactor.

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
  private record RankCacheKey(Long playerId, String queueType) {}
}
