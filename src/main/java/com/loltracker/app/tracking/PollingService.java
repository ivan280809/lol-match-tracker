package com.loltracker.app.tracking;

import com.loltracker.app.integration.riot.RiotClient;
import com.loltracker.app.match.MatchSummary;
import com.loltracker.app.match.TrackedMatchEntity;
import com.loltracker.app.match.TrackedMatchService;
import com.loltracker.app.notification.NotificationService;
import com.loltracker.app.ops.PollRunEntity;
import com.loltracker.app.ops.PollRunService;
import com.loltracker.app.player.PlayerEntity;
import com.loltracker.app.player.PlayerService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PollingService {

  private final PlayerService playerService;
  private final RiotClient riotClient;
  private final TrackedMatchService trackedMatchService;
  private final NotificationService notificationService;
  private final PollRunService pollRunService;

  private final AtomicBoolean running = new AtomicBoolean(false);

  @Scheduled(
      fixedDelayString = "${app.poll.fixed-delay:PT5M}",
      initialDelayString = "${app.poll.initial-delay:PT30S}")
  public void scheduledPoll() {
    runPoll();
  }

  public PollSummary runPoll() {
    if (!running.compareAndSet(false, true)) {
      log.info("Skipping poll because another run is still active");
      return new PollSummary(0, 0, 0, 0, "SKIPPED");
    }

    PollRunEntity run = pollRunService.startRun();
    int processed = 0;
    int newMatches = 0;
    int notifications = 0;
    List<String> playerErrors = new ArrayList<>();

    try {
      List<PlayerEntity> players = playerService.getActivePlayers();
      for (PlayerEntity player : players) {
        processed++;
        try {
          String puuid = playerService.ensurePuuid(player);
          notifications += retryPendingNotifications(player);
          List<String> matchIds = riotClient.fetchRecentMatchIds(puuid);
          for (String matchId : matchIds) {
            TrackedMatchEntity existingMatch =
                trackedMatchService.findExisting(player, matchId).orElse(null);
            if (existingMatch != null) {
              if (!existingMatch.isNotificationSent()) {
                notificationService.notifyNewMatch(existingMatch);
                trackedMatchService.markNotificationSent(existingMatch);
                notifications++;
              }
              continue;
            }
            MatchSummary summary = riotClient.fetchMatchSummary(matchId, puuid);
            TrackedMatchEntity trackedMatch = trackedMatchService.create(player, summary);
            newMatches++;
            notificationService.notifyNewMatch(trackedMatch);
            trackedMatchService.markNotificationSent(trackedMatch);
            notifications++;
          }
          playerService.updateSyncSuccess(player, puuid);
        } catch (Exception e) {
          log.warn("Player sync failed for {}#{}", player.getGameName(), player.getTagLine(), e);
          playerErrors.add(buildPlayerError(player, e));
          playerService.updateSyncFailure(player, e.getMessage());
        }
      }

      pollRunService.completeRun(run, processed, newMatches, notifications, playerErrors);
      return new PollSummary(
          processed,
          newMatches,
          notifications,
          playerErrors.size(),
          playerErrors.isEmpty() ? "SUCCESS" : "PARTIAL_SUCCESS");
    } catch (Exception e) {
      pollRunService.failRun(run, processed, newMatches, notifications, e.getMessage());
      throw e;
    } finally {
      running.set(false);
    }
  }

  private int retryPendingNotifications(PlayerEntity player) {
    int notificationsSent = 0;
    for (TrackedMatchEntity pendingMatch : trackedMatchService.getPendingNotifications(player)) {
      notificationService.notifyNewMatch(pendingMatch);
      trackedMatchService.markNotificationSent(pendingMatch);
      notificationsSent++;
    }
    return notificationsSent;
  }

  private String buildPlayerError(PlayerEntity player, Exception e) {
    String message = e.getMessage();
    if (message == null || message.isBlank()) {
      message = "Unknown error";
    }
    return player.getGameName() + "#" + player.getTagLine() + ": " + message;
  }
}
