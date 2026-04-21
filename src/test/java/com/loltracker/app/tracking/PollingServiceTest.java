package com.loltracker.app.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.loltracker.app.integration.riot.RiotClient;
import com.loltracker.app.match.MatchSummary;
import com.loltracker.app.match.TrackedMatchEntity;
import com.loltracker.app.match.TrackedMatchService;
import com.loltracker.app.notification.NotificationService;
import com.loltracker.app.ops.PollRunEntity;
import com.loltracker.app.ops.PollRunService;
import com.loltracker.app.player.PlayerEntity;
import com.loltracker.app.player.PlayerService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PollingServiceTest {

  @Mock private PlayerService playerService;
  @Mock private RiotClient riotClient;
  @Mock private TrackedMatchService trackedMatchService;
  @Mock private NotificationService notificationService;
  @Mock private PollRunService pollRunService;

  @InjectMocks private PollingService pollingService;

  @Test
  void runPollProcessesNewMatchesAndNotifies() {
    PlayerEntity player = new PlayerEntity();
    player.setId(10L);
    player.setGameName("Bazaga");
    player.setTagLine("ESP");

    MatchSummary summary =
        new MatchSummary("EUW1_123", "Lux", true, "CLASSIC", 1800, Instant.parse("2026-04-03T18:00:00Z"));
    TrackedMatchEntity trackedMatch = new TrackedMatchEntity();
    trackedMatch.setMatchId("EUW1_123");
    trackedMatch.setPlayer(player);

    when(playerService.getActivePlayers()).thenReturn(List.of(player));
    when(pollRunService.startRun()).thenReturn(new PollRunEntity());
    when(playerService.ensurePuuid(player)).thenReturn("puuid-1");
    when(trackedMatchService.getPendingNotifications(player)).thenReturn(List.of());
    when(riotClient.fetchRecentMatchIds("puuid-1")).thenReturn(List.of("EUW1_123"));
    when(trackedMatchService.findExisting(player, "EUW1_123")).thenReturn(Optional.empty());
    when(riotClient.fetchMatchSummary("EUW1_123", "puuid-1")).thenReturn(summary);
    when(trackedMatchService.create(player, summary)).thenReturn(trackedMatch);

    PollSummary result = pollingService.runPoll();

    assertEquals(1, result.playersProcessed());
    assertEquals(1, result.newMatchesFound());
    assertEquals(1, result.notificationsSent());
    assertEquals(0, result.playerFailures());
    assertEquals("SUCCESS", result.status());
    verify(notificationService).notifyNewMatch(trackedMatch);
    verify(trackedMatchService).markNotificationSent(trackedMatch);
    verify(playerService).updateSyncSuccess(player, "puuid-1");
  }

  @Test
  void runPollResolvesLegacyPlayerPuuidBeforeFetchingMatches() {
    PlayerEntity player = new PlayerEntity();
    player.setId(11L);
    player.setGameName("Bazaga");
    player.setTagLine("ESP");

    when(playerService.getActivePlayers()).thenReturn(List.of(player));
    when(pollRunService.startRun()).thenReturn(new PollRunEntity());
    when(playerService.ensurePuuid(player)).thenReturn("puuid-legacy");
    when(trackedMatchService.getPendingNotifications(player)).thenReturn(List.of());
    when(riotClient.fetchRecentMatchIds("puuid-legacy")).thenReturn(List.of());

    PollSummary result = pollingService.runPoll();

    assertEquals(1, result.playersProcessed());
    assertEquals(0, result.newMatchesFound());
    assertEquals(0, result.notificationsSent());
    assertEquals(0, result.playerFailures());
    assertEquals("SUCCESS", result.status());
    verify(playerService).ensurePuuid(player);
    verify(playerService).updateSyncSuccess(player, "puuid-legacy");
    verifyNoMoreInteractions(notificationService);
  }

  @Test
  void runPollRetriesPendingNotificationsBeforeCheckingNewMatches() {
    PlayerEntity player = new PlayerEntity();
    player.setId(12L);
    player.setGameName("Bazaga");
    player.setTagLine("ESP");

    TrackedMatchEntity pendingMatch = new TrackedMatchEntity();
    pendingMatch.setMatchId("EUW1_777");
    pendingMatch.setPlayer(player);
    pendingMatch.setNotificationSent(false);

    when(playerService.getActivePlayers()).thenReturn(List.of(player));
    when(pollRunService.startRun()).thenReturn(new PollRunEntity());
    when(playerService.ensurePuuid(player)).thenReturn("puuid-1");
    when(trackedMatchService.getPendingNotifications(player)).thenReturn(List.of(pendingMatch));
    when(riotClient.fetchRecentMatchIds("puuid-1")).thenReturn(List.of("EUW1_777"));
    when(trackedMatchService.findExisting(player, "EUW1_777")).thenReturn(Optional.of(pendingMatch));
    doAnswer(
            invocation -> {
              pendingMatch.setNotificationSent(true);
              return null;
            })
        .when(trackedMatchService)
        .markNotificationSent(pendingMatch);

    PollSummary result = pollingService.runPoll();

    assertEquals(0, result.newMatchesFound());
    assertEquals(1, result.notificationsSent());
    assertEquals(0, result.playerFailures());
    assertEquals("SUCCESS", result.status());
    verify(notificationService).notifyNewMatch(pendingMatch);
    verify(trackedMatchService).markNotificationSent(pendingMatch);
    verify(riotClient, never()).fetchMatchSummary(anyString(), anyString());
  }

  @Test
  void runPollMarksPartialSuccessWhenAPlayerFails() {
    PlayerEntity okPlayer = new PlayerEntity();
    okPlayer.setId(20L);
    okPlayer.setGameName("Bazaga");
    okPlayer.setTagLine("ESP");

    PlayerEntity failedPlayer = new PlayerEntity();
    failedPlayer.setId(21L);
    failedPlayer.setGameName("Broken");
    failedPlayer.setTagLine("EUW");

    when(playerService.getActivePlayers()).thenReturn(List.of(okPlayer, failedPlayer));
    when(pollRunService.startRun()).thenReturn(new PollRunEntity());

    when(playerService.ensurePuuid(okPlayer)).thenReturn("puuid-ok");
    when(trackedMatchService.getPendingNotifications(okPlayer)).thenReturn(List.of());
    when(riotClient.fetchRecentMatchIds("puuid-ok")).thenReturn(List.of());

    when(playerService.ensurePuuid(failedPlayer)).thenReturn("puuid-failed");
    when(trackedMatchService.getPendingNotifications(failedPlayer)).thenReturn(List.of());
    when(riotClient.fetchRecentMatchIds("puuid-failed"))
        .thenThrow(new IllegalStateException("Telegram timeout"));

    PollSummary result = pollingService.runPoll();

    assertEquals(2, result.playersProcessed());
    assertEquals(0, result.newMatchesFound());
    assertEquals(0, result.notificationsSent());
    assertEquals(1, result.playerFailures());
    assertEquals("PARTIAL_SUCCESS", result.status());
    verify(playerService).updateSyncSuccess(okPlayer, "puuid-ok");
    verify(playerService).updateSyncFailure(failedPlayer, "Telegram timeout");
    verify(pollRunService)
        .completeRun(any(), eq(2), eq(0), eq(0), argThat(errors -> errors.size() == 1));
  }

  @Test
  void runPollReturnsSkippedWhenAnotherRunIsActive() {
    AtomicBoolean running =
        (AtomicBoolean) ReflectionTestUtils.getField(pollingService, "running");
    running.set(true);

    PollSummary result = pollingService.runPoll();

    assertEquals(0, result.playersProcessed());
    assertEquals(0, result.newMatchesFound());
    assertEquals(0, result.notificationsSent());
    assertEquals(0, result.playerFailures());
    assertEquals("SKIPPED", result.status());
    verifyNoInteractions(playerService, riotClient, trackedMatchService, notificationService, pollRunService);
  }

  @Test
  void runPollMarksRunAsFailedWhenLoadingPlayersFails() {
    PollRunEntity run = new PollRunEntity();
    when(pollRunService.startRun()).thenReturn(run);
    when(playerService.getActivePlayers()).thenThrow(new IllegalStateException("Repository unavailable"));

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> pollingService.runPoll());

    assertEquals("Repository unavailable", exception.getMessage());
    verify(pollRunService).failRun(run, 0, 0, 0, "Repository unavailable");

    AtomicBoolean running =
        (AtomicBoolean) ReflectionTestUtils.getField(pollingService, "running");
    assertFalse(running.get());
  }
}
