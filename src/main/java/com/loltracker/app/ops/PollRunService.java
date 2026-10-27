package com.loltracker.app.ops;

import com.loltracker.app.player.PlayerEntity;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class PollRunService {

  private final PollRunRepository pollRunRepository;
  private final Clock clock;
  @Autowired
  private TransactionTemplate transactionTemplate;


  private final Map<Long, CachedProgress> progressCache = new ConcurrentHashMap<>();

  private static final long PROGRESS_FLUSH_INTERVAL = 5L;

  @Autowired(required = false)
  @org.springframework.beans.factory.annotation.Qualifier("progressScheduler")
  private TaskScheduler taskScheduler;

  @Transactional
  public PollRunEntity startRun() {
    return startRun(null);
  }

  @Transactional
  public PollRunEntity startRun(String owner) {
    PollRunEntity entity = new PollRunEntity();
    entity.setStartedAt(now());
    entity.setStatus("RUNNING");
    entity.setOwner(owner);
    entity.setCurrentStage("Inicializando");
    return pollRunRepository.save(entity);
  }

  @Transactional
  public void updateProgress(PollRunEntity entity, PlayerEntity player, String stage) {
    if (entity.getId() == null) {
      return;
    }
    if (entity.getStatus() != null && !"RUNNING".equals(entity.getStatus())) {
      return;
    }
    String playerText = player == null ? null : player.getGameName() + "#" + player.getTagLine();
    String stageText = shortText(stage, 120);
    CachedProgress cached = progressCache.computeIfAbsent(entity.getId(), id -> new CachedProgress());
    synchronized (cached) {
      cached.pendingStage = stageText;
      cached.pendingPlayer = playerText;

      Instant current = now();
      String effectiveStage = cached.inFlightToken == null ? cached.lastPersistedStage : cached.inFlightStage;
      Instant effectiveFlushAt = cached.inFlightToken == null ? cached.lastFlushAt : cached.inFlightAt;
      boolean stageChanged = !java.util.Objects.equals(effectiveStage, stageText);
      boolean intervalExceeded = effectiveFlushAt == null
          || Duration.between(effectiveFlushAt, current)
              .compareTo(Duration.ofSeconds(PROGRESS_FLUSH_INTERVAL)) >= 0;
      if (intervalExceeded || stageChanged) {
        flushProgress(entity, cached, current);
      } else {
        scheduleProgressFlush(entity.getId(), cached);
      }
    }
  }

  private void flushProgress(PollRunEntity fallback, CachedProgress cached, Instant flushedAt) {
    String flushedStage = cached.pendingStage;
    String flushedPlayer = cached.pendingPlayer;
    Object flushedToken = new Object();
    cached.inFlightToken = flushedToken;
    cached.inFlightStage = flushedStage;
    cached.inFlightAt = flushedAt;
    PollRunEntity managed = pollRunRepository.findByIdForUpdate(fallback.getId()).orElse(fallback);
    if (!"RUNNING".equals(managed.getStatus())) {
      progressCache.remove(fallback.getId(), cached);
      cached.pendingStage = null;
      cached.pendingPlayer = null;
      cached.inFlightToken = null;
      return;
    }
    managed.setCurrentStage(flushedStage);
    managed.setCurrentPlayer(flushedPlayer);
    pollRunRepository.save(managed);
    if (java.util.Objects.equals(cached.pendingStage, flushedStage)
        && java.util.Objects.equals(cached.pendingPlayer, flushedPlayer)) {
      cached.pendingStage = null;
      cached.pendingPlayer = null;
    }
    Runnable committed = () -> progressFlushCommitted(fallback.getId(), cached, flushedStage, flushedPlayer, flushedAt, flushedToken);
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              committed.run();
            }

            @Override
            public void afterCompletion(int status) {
              if (status != TransactionSynchronization.STATUS_COMMITTED) {
                synchronized (cached) {
                  if (cached.inFlightToken == flushedToken) {
                    cached.inFlightToken = null;
                    cached.inFlightStage = null;
                    cached.inFlightAt = null;
                  }
                  cached.lastFlushAt = null;
                  if (cached.scheduledFlush != null) {
                    cached.scheduledFlush.cancel(false);
                    cached.scheduledFlush = null;
                  }
                }
              }
            }
          });
    } else {
      committed.run();
    }
  }


  private void flushPendingProgress(Long runId) {
    if (runId == null) {
      return;
    }
    CachedProgress cached = progressCache.get(runId);
    if (cached == null) {
      return;
    }
    synchronized (cached) {
      cached.scheduledFlush = null;
      if (cached.pendingStage == null) {
        return;
      }
      Runnable persist = () -> {
        PollRunEntity fallback = new PollRunEntity();
        fallback.setId(runId);
        flushProgress(fallback, cached, now());
      };
      try {
        if (transactionTemplate != null) {
          transactionTemplate.executeWithoutResult(status -> persist.run());
        } else {
          persist.run();
        }
      } catch (RuntimeException failure) {
        scheduleProgressRetry(runId, cached);
      }
    }
  }

  private void progressFlushCommitted(
      Long runId, CachedProgress cached, String stage, String player, Instant flushedAt, Object flushedToken) {
    synchronized (cached) {
      if (cached.inFlightToken != flushedToken) {
        return;
      }
      cached.lastPersistedStage = stage;
      cached.lastFlushAt = flushedAt;
      if (java.util.Objects.equals(cached.pendingStage, stage)
          && java.util.Objects.equals(cached.pendingPlayer, player)) {
        cached.pendingStage = null;
        cached.pendingPlayer = null;
      }
      cached.inFlightToken = null;
      cached.inFlightStage = null;
      cached.inFlightAt = null;
      if (cached.pendingStage != null) {
        scheduleProgressFlush(runId, cached);
      }
    }
  }

  private void scheduleProgressFlush(Long runId, CachedProgress cached) {
    if (taskScheduler == null || cached.pendingStage == null || cached.scheduledFlush != null) {
      return;
    }
    Instant base = cached.inFlightAt == null ? cached.lastFlushAt : cached.inFlightAt;
    // Reserve 500 ms for scheduling and committing before the visibility deadline.
    Instant deadline = (base == null ? now() : base)
        .plusSeconds(PROGRESS_FLUSH_INTERVAL).minusMillis(500);
    cached.scheduledFlush = taskScheduler.schedule(() -> flushPendingProgress(runId), deadline);
  }

  private void scheduleProgressRetry(Long runId, CachedProgress cached) {
    if (taskScheduler == null || cached.pendingStage == null) {
      return;
    }
    if (cached.scheduledFlush != null) {
      cached.scheduledFlush.cancel(false);
    }
    cached.scheduledFlush = taskScheduler.schedule(() -> flushPendingProgress(runId), now().plusSeconds(1));
  }

  @Transactional
  public PollRunEntity completeRun(
      PollRunEntity entity,
      int playersProcessed,
      int newMatchesFound,
      int notificationsSent,
      List<String> playerErrors) {
    lockTerminalTransition(entity);
    entity.setFinishedAt(now());
    entity.setStatus(playerErrors.isEmpty() ? "SUCCESS" : "PARTIAL_SUCCESS");
    entity.setPlayersProcessed(playersProcessed);
    entity.setNewMatchesFound(newMatchesFound);
    entity.setNotificationsSent(notificationsSent);
    entity.setErrorSummary(playerErrors.isEmpty() ? null : buildPlayerErrorSummary(playerErrors));
    entity.setCurrentStage(playerErrors.isEmpty() ? "Finalizado" : "Finalizado con errores");
    entity.setCurrentPlayer(null);
    PollRunEntity saved = pollRunRepository.save(entity);
    clearProgressAfterCommit(entity);
    return saved;
  }


  @Transactional
  public PollRunEntity failRun(
      PollRunEntity entity,
      int playersProcessed,
      int newMatchesFound,
      int notificationsSent,
      String errorSummary) {
    lockTerminalTransition(entity);
    entity.setFinishedAt(now());
    entity.setStatus("ERROR");
    entity.setPlayersProcessed(playersProcessed);
    entity.setNewMatchesFound(newMatchesFound);
    entity.setNotificationsSent(notificationsSent);
    entity.setErrorSummary(errorSummary == null ? null : errorSummary.substring(0, Math.min(500, errorSummary.length())));
    entity.setCurrentStage("Error");
    entity.setCurrentPlayer(null);
    PollRunEntity saved = pollRunRepository.save(entity);
    clearProgressAfterCommit(entity);
    return saved;
  }


  @Transactional
  public PollRunEntity failRun(
      PollRunEntity entity,
      int playersProcessed,
      int newMatchesFound,
      int notificationsSent,
      List<String> playerErrors) {
    String joined = playerErrors == null ? null : String.join(", ", playerErrors);
    return failRun(entity, playersProcessed, newMatchesFound, notificationsSent, joined);
  }


  @Transactional
  public PollRunEntity rateLimited(
      PollRunEntity entity,
      int playersProcessed,
      int newMatchesFound,
      int notificationsSent,
      java.time.Instant pausedUntil,
      String message) {
    lockTerminalTransition(entity);
    entity.setFinishedAt(now());
    entity.setStatus("RATE_LIMITED");
    entity.setPlayersProcessed(playersProcessed);
    entity.setNewMatchesFound(newMatchesFound);
    entity.setNotificationsSent(notificationsSent);
    entity.setRateLimitPausedUntil(pausedUntil);
    entity.setRateLimitMessage(message);
    entity.setErrorSummary(message == null ? null : message.substring(0, Math.min(500, message.length())));
    entity.setCurrentStage("Pausado por rate limit Riot");
    entity.setCurrentPlayer(null);
    PollRunEntity saved = pollRunRepository.save(entity);
    clearProgressAfterCommit(entity);
    return saved;
  }

  @Transactional(readOnly = true)
  public List<PollRunView> getRecentRuns() {
    return pollRunRepository.findTop10ByOrderByStartedAtDesc().stream()
        .map(PollRunView::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<PollRunView> getRecentRateLimitRuns() {
    return pollRunRepository.findTop5ByRateLimitPausedUntilIsNotNullOrderByStartedAtDesc().stream()
        .map(PollRunView::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public Optional<PollRunView> getActiveRun() {
    return pollRunRepository.findFirstByStatusOrderByStartedAtDesc("RUNNING").map(PollRunView::fromEntity);
  }

  @Transactional(readOnly = true)
  public Optional<PollRunView> getActiveRateLimitPause() {
    return pollRunRepository
        .findFirstByRateLimitPausedUntilAfterOrderByRateLimitPausedUntilDesc(now())
        .map(PollRunView::fromEntity);
  }

  @Transactional(readOnly = true)
  public boolean isRateLimitPaused() {
    return getActiveRateLimitPause().isPresent();
  }

  @Transactional(readOnly = true)
  public boolean isScheduledRunDue(Duration fixedDelay) {
    Optional<PollRunEntity> lastFinished =
        pollRunRepository.findFirstByFinishedAtIsNotNullOrderByFinishedAtDesc();
    if (lastFinished.isEmpty()) {
      return true;
    }
    return !lastFinished.get().getFinishedAt().plus(fixedDelay).isAfter(now());
  }

  private String buildPlayerErrorSummary(List<String> playerErrors) {
    String prefix =
        playerErrors.size() == 1
            ? "1 player failed: "
            : playerErrors.size() + " players failed: ";
    String joined = String.join("; ", playerErrors);
    String summary = prefix + joined;
    return summary.substring(0, Math.min(500, summary.length()));
  }


  private void lockTerminalTransition(PollRunEntity entity) {
    if (entity == null || entity.getId() == null) {
      return;
    }
    pollRunRepository.findByIdForUpdate(entity.getId());
  }

  private String shortText(String value, int maxLength) {
    if (value == null) {
      return null;
    }
    return value.substring(0, Math.min(maxLength, value.length()));
  }

  private Instant now() {
    return clock == null ? Instant.now() : clock.instant();
  }

  private void clearProgressAfterCommit(PollRunEntity entity) {
    if (entity.getId() != null) {
      Runnable clear = () -> {
        CachedProgress cached = progressCache.remove(entity.getId());
        if (cached != null && cached.scheduledFlush != null) {
          cached.scheduledFlush.cancel(false);
        }
      };
      if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
              @Override
              public void afterCommit() {
                clear.run();
              }
            });
      } else {
        clear.run();
      }
    }
  }


  private static final class CachedProgress {
    private String lastPersistedStage;
    private String pendingStage;
    private String pendingPlayer;
    private Instant lastFlushAt;
    private Object inFlightToken;
    private String inFlightStage;
    private Instant inFlightAt;
    private ScheduledFuture<?> scheduledFlush;
  }
}
