package com.loltracker.app.notification;

import com.loltracker.app.player.PlayerEntity;
import com.loltracker.app.player.PlayerService;
import com.loltracker.app.notification.NotificationDispatchResult;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationOutboxDispatcherService {

  private final NotificationService notificationService;
  private final PlayerService playerService;
  private final AtomicBoolean dispatchRunning = new AtomicBoolean();

  // Configurable limits for dispatcher runs
  @Value("${app.notification.dispatch.maxMessagesPerRun:100}")
  private int maxMessagesPerRun;

  @Value("${app.notification.dispatch.maxDurationMillis:90000}")
  private long maxDurationMillis;

  // Default delay of 30 seconds between dispatch runs; can be overridden via app.notification.dispatch.delay
  // In Spring 6 the @Scheduled annotation uses `scheduler` instead of `schedulerRef`.
  // The scheduler bean is defined in SchedulerConfig as `dispatcherScheduler`.
  @Scheduled(fixedDelayString = "${app.notification.dispatch.delay:PT30S}", scheduler = "dispatcherScheduler")
  public void runDispatch() {
    if (!dispatchRunning.compareAndSet(false, true)) {
      log.debug("Skipping notification dispatch because a local run is already active");
      return;
    }
    try {
      runDispatchPass();
    } finally {
      dispatchRunning.set(false);
    }
  }

  private void runDispatchPass() {
    long startNanos = System.nanoTime();
    int messagesAttempted = 0;
    if (maxMessagesPerRun <= 0 || maxDurationMillis <= 0) {
      return;
    }
    long minimumSendWindowNanos = 0L;
    long deadlineNanos = startNanos + maxDurationMillis * 1_000_000L;
    for (PlayerEntity player : playerService.getActivePlayers()) {
      if (messagesAttempted >= maxMessagesPerRun) {
        log.debug("Reached maxMessagesPerRun limit of {}", maxMessagesPerRun);
        break;
      }
      // Check the overall run deadline before starting a new player.
      if (deadlineNanos - System.nanoTime() < minimumSendWindowNanos) {
        log.debug("Reached maxDurationMillis limit of {}ms", maxDurationMillis);
        break;
      }
      try {
        int remaining = maxMessagesPerRun - messagesAttempted;
        NotificationDispatchResult result =
            notificationService.dispatchPendingForPlayer(
                player, remaining, deadlineNanos, minimumSendWindowNanos);
        messagesAttempted += result.sent() + result.failed();
        log.debug("Dispatched {} notifications for player {}#{}", result.sent(), player.getGameName(), player.getTagLine());
        if (result.rateLimited()) {
          log.info("Stopping this dispatcher pass after Telegram rate limiting");
          break;
        }
      } catch (Exception e) {
        // An exception indicates a partial failure inside the player dispatch call.
        // We conservatively abort the remainder of this dispatch pass to avoid
        // exceeding the per‑run message budget or attempting to send more
        // notifications than intended.
        log.warn("Failed to dispatch notifications for player {}#{}", player.getGameName(), player.getTagLine(), e);
        break;
      }
    }
    log.info("Notification dispatcher run completed: {} messages attempted in {}ms", messagesAttempted, (System.nanoTime() - startNanos) / 1_000_000L);
  }

}
