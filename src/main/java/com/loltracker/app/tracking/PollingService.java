package com.loltracker.app.tracking;

import com.loltracker.app.integration.riot.RiotApiException;
import com.loltracker.app.integration.riot.RiotErrorCategory;
import com.loltracker.app.integration.riot.RiotMatchDetails;
import com.loltracker.app.integration.riot.RiotMatchPort;
import com.loltracker.app.match.MatchSummary;
import com.loltracker.app.match.TrackedMatchEntity;
import com.loltracker.app.match.TrackedMatchService;
import com.loltracker.app.notification.NotificationDispatchResult;
import com.loltracker.app.notification.NotificationService;
import com.loltracker.app.ops.PollLease;
import com.loltracker.app.ops.PollLockService;
import com.loltracker.app.ops.PollRunEntity;
import com.loltracker.app.ops.PollRunService;
import com.loltracker.app.ops.OpsMetrics;
import com.loltracker.app.player.PlayerEntity;
import com.loltracker.app.player.PlayerService;
import com.loltracker.app.settings.AppConfigurationService;
import com.loltracker.app.settings.RuntimeAppConfiguration;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PollingService {

  private static final Duration POLL_LOCK_LEASE = Duration.ofMinutes(20);
  private static final Duration DEFAULT_RATE_LIMIT_PAUSE = Duration.ofMinutes(2);

  private final PlayerService playerService;
  private final RiotMatchPort riotMatchPort;
  private final TrackedMatchService trackedMatchService;
  private final NotificationService notificationService;
  private final PollRunService pollRunService;
  private final PollLockService pollLockService;
  private final AppConfigurationService appConfigurationService;
  private final Clock clock;
  private final OpsMetrics opsMetrics;

  private final AtomicBoolean running = new AtomicBoolean(false);

  @Scheduled(
      fixedDelayString = "${app.poll.scheduler-tick:PT30S}",
      initialDelayString = "${app.poll.initial-delay:PT30S}")
  public void scheduledPoll() {
    RuntimeAppConfiguration configuration = appConfigurationService.getRuntimeConfiguration();
    if (!configuration.pollingEnabled() || configuration.pollingManualOnly()) {
      return;
    }
    if (pollRunService.isRateLimitPaused()) {
      return;
    }
    if (!pollRunService.isScheduledRunDue(configuration.pollingFixedDelay())) {
      return;
    }
    runPoll();
  }

  public PollSummary runPoll() {
    long startedNanos = System.nanoTime();
    if (!running.compareAndSet(false, true)) {
      log.info("Skipping poll because another local run is still active");
      return finish(new PollSummary(0, 0, 0, 0, "SKIPPED"), startedNanos);
    }

    if (pollRunService.isRateLimitPaused()) {
      running.set(false);
      log.info("Skipping poll because Riot rate-limit pause is active");
      return finish(new PollSummary(0, 0, 0, 0, "RATE_LIMITED"), startedNanos);
    }

    PollLease lease = null;
    PollRunEntity run = null;
    int processed = 0;
    int newMatches = 0;
    int notifications = 0;
    List<String> playerErrors = new ArrayList<>();
    Map<String, RiotMatchDetails> matchCache = new HashMap<>();

    try {
      Optional<PollLease> acquiredLease =
          pollLockService == null
              ? Optional.of(new PollLease("unit-test", now().plus(POLL_LOCK_LEASE)))
              : pollLockService.acquire(POLL_LOCK_LEASE);
      if (acquiredLease.isEmpty()) {
        log.info("Skipping poll because another instance owns the DB poll lock");
        return finish(new PollSummary(0, 0, 0, 0, "SKIPPED"), startedNanos);
      }
      lease = acquiredLease.get();
      run = pollLockService == null ? pollRunService.startRun() : pollRunService.startRun(lease.owner());
      RuntimeAppConfiguration configuration = runtimeConfiguration();
      List<PlayerEntity> players = playerService.getActivePlayers();
      for (PlayerEntity player : players) {
        processed++;
        try {
          PlayerPollResult playerResult = pollPlayer(player, run, configuration, matchCache);
          newMatches += playerResult.newMatches();
          notifications += playerResult.notifications();
        } catch (RiotApiException e) {
          if (e.category() == RiotErrorCategory.RATE_LIMIT) {
            Instant pausedUntil = now().plus(rateLimitPause(e));
            String message = friendlyRiotMessage(e);
            pollRunService.rateLimited(run, processed, newMatches, notifications, pausedUntil, message);
            recordRateLimitPause(rateLimitPause(e));
            playerService.updateSyncFailure(player, message);
            return finish(
                new PollSummary(processed, newMatches, notifications, playerErrors.size() + 1, "RATE_LIMITED"),
                startedNanos);
          }
          log.warn("Player sync failed for {}#{}", player.getGameName(), player.getTagLine(), e);
          String message = friendlyRiotMessage(e);
          playerErrors.add(buildPlayerError(player, message));
          playerService.updateSyncFailure(player, message);
        } catch (Exception e) {
          log.warn("Player sync failed for {}#{}", player.getGameName(), player.getTagLine(), e);
          String message = friendlyMessage(e);
          playerErrors.add(buildPlayerError(player, message));
          playerService.updateSyncFailure(player, message);
        }
      }

      pollRunService.completeRun(run, processed, newMatches, notifications, playerErrors);
      return finish(
          new PollSummary(
              processed,
              newMatches,
              notifications,
              playerErrors.size(),
              playerErrors.isEmpty() ? "SUCCESS" : "PARTIAL_SUCCESS"),
          startedNanos);
    } catch (Exception e) {
      if (run != null) {
        pollRunService.failRun(run, processed, newMatches, notifications, friendlyMessage(e));
      }
      recordPollRun("ERROR", startedNanos);
      throw e;
    } finally {
      if (lease != null && pollLockService != null) {
        pollLockService.release(lease);
      }
      running.set(false);
    }
  }

  private PlayerPollResult pollPlayer(
      PlayerEntity player,
      PollRunEntity run,
      RuntimeAppConfiguration configuration,
      Map<String, RiotMatchDetails> matchCache) {
    int newMatches = 0;
    int notifications = 0;

    pollRunService.updateProgress(run, player, "Resolviendo identidad Riot");
    String puuid = playerService.ensurePuuid(player);
    enqueueLegacyPendingNotifications(player);
    notifications += dispatchPendingNotifications(player);

    int pageSize = configuration.pollingMatchWindowSize();
    int pageLimit = configuration.pollingPaginationLimit();
    boolean stopPagination = false;
    for (int page = 0; page < pageLimit && !stopPagination; page++) {
      int start = page * pageSize;
      pollRunService.updateProgress(run, player, "Consultando historial Riot " + (page + 1) + "/" + pageLimit);
      List<String> matchIds = fetchMatchIds(puuid, start, pageSize);
      if (matchIds.isEmpty()) {
        break;
      }
      for (String matchId : matchIds) {
        if (trackedMatchService.findExisting(player, matchId).isPresent()) {
          stopPagination = true;
          break;
        }

        MatchSummary summary = fetchSummary(matchId, puuid, matchCache);
        if (!shouldImportMatch(player, summary)) {
          stopPagination = true;
          break;
        }

        boolean suppressNotification = !shouldNotifyMatch(player, summary);
        TrackedMatchEntity trackedMatch =
            suppressNotification
                ? trackedMatchService.create(player, summary, true)
                : trackedMatchService.create(player, summary);
        newMatches++;
        if (!suppressNotification) {
          notificationService.enqueueMatchNotification(trackedMatch);
        }
      }
      if (matchIds.size() < pageSize) {
        stopPagination = true;
      }
    }

    pollRunService.updateProgress(run, player, "Enviando avisos pendientes");
    notifications += dispatchPendingNotifications(player);
    playerService.updateSyncSuccess(player, puuid);
    return new PlayerPollResult(newMatches, notifications);
  }

  private RuntimeAppConfiguration runtimeConfiguration() {
    return appConfigurationService == null
        ? new RuntimeAppConfiguration("", com.loltracker.app.settings.RiotRegion.EUROPE, "", "")
        : appConfigurationService.getRuntimeConfiguration();
  }

  private void enqueueLegacyPendingNotifications(PlayerEntity player) {
    Optional.ofNullable(trackedMatchService.getPendingNotifications(player)).orElseGet(List::of).stream()
        .forEach(notificationService::enqueueMatchNotification);
  }

  private List<String> fetchMatchIds(String puuid, int start, int pageSize) {
    if (start == 0 && pageSize == 10) {
      List<String> matchIds = riotMatchPort.fetchRecentMatchIds(puuid);
      if (matchIds != null) {
        return matchIds;
      }
    }
    return riotMatchPort.fetchRecentMatchIds(puuid, start, pageSize);
  }

  private MatchSummary fetchSummary(
      String matchId, String puuid, Map<String, RiotMatchDetails> matchCache) {
    RiotMatchDetails details = matchCache.computeIfAbsent(matchId, riotMatchPort::fetchMatchDetails);
    if (details == null) {
      return riotMatchPort.fetchMatchSummary(matchId, puuid);
    }
    return details.summaryFor(puuid);
  }

  private boolean shouldImportMatch(PlayerEntity player, MatchSummary summary) {
    if (player.getTrackFrom() == null || summary.gameEndAt() == null) {
      return true;
    }
    return playerService.shouldImportMatch(player, summary);
  }

  private boolean shouldNotifyMatch(PlayerEntity player, MatchSummary summary) {
    if (player.getTrackFrom() == null || summary.gameEndAt() == null) {
      return true;
    }
    return playerService.shouldNotifyMatch(player, summary);
  }

  private int dispatchPendingNotifications(PlayerEntity player) {
    NotificationDispatchResult result = notificationService.dispatchPendingForPlayer(player);
    if (result.failed() > 0) {
      log.warn(
          "{} notification deliveries failed for {}#{}; matches remain queued",
          result.failed(),
          player.getGameName(),
          player.getTagLine());
    }
    return result.sent();
  }

  private Duration rateLimitPause(RiotApiException e) {
    return e.retryAfter() == null || e.retryAfter().isNegative() || e.retryAfter().isZero()
        ? DEFAULT_RATE_LIMIT_PAUSE
        : e.retryAfter();
  }

  private String buildPlayerError(PlayerEntity player, String message) {
    return player.getGameName() + "#" + player.getTagLine() + ": " + message;
  }

  private String friendlyMessage(Exception e) {
    if (e instanceof RiotApiException riotApiException) {
      return friendlyRiotMessage(riotApiException);
    }
    String message = e.getMessage();
    if (message == null || message.isBlank()) {
      message = e.getClass().getSimpleName();
    }
    return message;
  }

  private String friendlyRiotMessage(RiotApiException e) {
    return switch (e.category()) {
      case NOT_CONFIGURED -> "Riot API key no configurada";
      case UNAUTHORIZED -> "Riot API key invalida o sin permisos";
      case NOT_FOUND -> "Cuenta Riot no encontrada";
      case RATE_LIMIT -> "Riot rate limit activo. Reintentar tras " + rateLimitPause(e).toSeconds() + "s";
      case WRONG_REGION_OR_PLATFORM -> "Region o plataforma Riot incorrecta";
      case TIMEOUT -> "Timeout llamando a Riot";
      case MALFORMED_RESPONSE -> "Riot devolvio una respuesta mal formada";
      case UNAVAILABLE -> "Riot no esta disponible temporalmente";
      case UNKNOWN -> e.getMessage() == null || e.getMessage().isBlank() ? "Error desconocido de Riot" : e.getMessage();
    };
  }

  private Instant now() {
    return clock == null ? Instant.now() : clock.instant();
  }

  private PollSummary finish(PollSummary summary, long startedNanos) {
    recordPollRun(summary.status(), startedNanos);
    return summary;
  }

  private void recordPollRun(String status, long startedNanos) {
    if (opsMetrics != null) {
      opsMetrics.recordPollRun(status, System.nanoTime() - startedNanos);
    }
  }

  private void recordRateLimitPause(Duration duration) {
    if (opsMetrics != null) {
      opsMetrics.recordRateLimitPause(duration);
    }
  }

  private record PlayerPollResult(int newMatches, int notifications) {}
}
