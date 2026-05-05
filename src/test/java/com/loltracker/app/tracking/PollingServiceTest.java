package com.loltracker.app.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.loltracker.app.integration.riot.RiotClient;
import com.loltracker.app.match.MatchSummary;
import com.loltracker.app.match.TrackedMatchEntity;
import com.loltracker.app.match.TrackedMatchService;
import com.loltracker.app.notification.NotificationDispatchResult;
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
    when(notificationService.dispatchPendingForPlayer(player))
        .thenReturn(NotificationDispatchResult.empty())
        .thenReturn(new NotificationDispatchResult(1, 0));
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
    verify(notificationService).enqueueMatchNotification(trackedMatch);
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
    when(notificationService.dispatchPendingForPlayer(player)).thenReturn(NotificationDispatchResult.empty());
    when(riotClient.fetchRecentMatchIds("puuid-legacy")).thenReturn(List.of());

    PollSummary result = pollingService.runPoll();

    assertEquals(1, result.playersProcessed());
    assertEquals(0, result.newMatchesFound());
    assertEquals(0, result.notificationsSent());
    assertEquals(0, result.playerFailures());
    assertEquals("SUCCESS", result.status());
    verify(playerService).ensurePuuid(player);
    verify(playerService).updateSyncSuccess(player, "puuid-legacy");
    verify(notificationService, times(2)).dispatchPendingForPlayer(player);
  }

  @Test
  void runPollDispatchesPendingNotificationsBeforeCheckingNewMatches() {
    PlayerEntity player = new PlayerEntity();
    player.setId(12L);
    player.setGameName("Bazaga");
    player.setTagLine("ESP");

    when(playerService.getActivePlayers()).thenReturn(List.of(player));
    when(pollRunService.startRun()).thenReturn(new PollRunEntity());
    when(playerService.ensurePuuid(player)).thenReturn("puuid-1");
    when(notificationService.dispatchPendingForPlayer(player))
        .thenReturn(new NotificationDispatchResult(1, 0))
        .thenReturn(NotificationDispatchResult.empty());
    when(riotClient.fetchRecentMatchIds("puuid-1")).thenReturn(List.of());

    PollSummary result = pollingService.runPoll();

    assertEquals(0, result.newMatchesFound());
    assertEquals(1, result.notificationsSent());
    assertEquals(0, result.playerFailures());
    assertEquals("SUCCESS", result.status());
    verify(notificationService, times(2)).dispatchPendingForPlayer(player);
    verify(riotClient, never()).fetchMatchSummary(anyString(), anyString());
  }

  @Test
  void runPollEnqueuesLegacyPendingNotificationsBeforeDispatch() {
    PlayerEntity player = new PlayerEntity();
    player.setId(13L);
    player.setGameName("Bazaga");
    player.setTagLine("ESP");

    TrackedMatchEntity legacyPendingMatch = new TrackedMatchEntity();
    legacyPendingMatch.setId(80L);
    legacyPendingMatch.setMatchId("EUW1_OLD");
    legacyPendingMatch.setPlayer(player);
    legacyPendingMatch.setNotificationSent(false);

    when(playerService.getActivePlayers()).thenReturn(List.of(player));
    when(pollRunService.startRun()).thenReturn(new PollRunEntity());
    when(playerService.ensurePuuid(player)).thenReturn("puuid-1");
    when(trackedMatchService.getPendingNotifications(player)).thenReturn(List.of(legacyPendingMatch));
    when(notificationService.dispatchPendingForPlayer(player))
        .thenReturn(new NotificationDispatchResult(1, 0))
        .thenReturn(NotificationDispatchResult.empty());
    when(riotClient.fetchRecentMatchIds("puuid-1")).thenReturn(List.of());

    PollSummary result = pollingService.runPoll();

    assertEquals(1, result.notificationsSent());
    assertEquals("SUCCESS", result.status());
    verify(notificationService).enqueueMatchNotification(legacyPendingMatch);
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
    when(notificationService.dispatchPendingForPlayer(okPlayer)).thenReturn(NotificationDispatchResult.empty());
    when(riotClient.fetchRecentMatchIds("puuid-ok")).thenReturn(List.of());

    when(playerService.ensurePuuid(failedPlayer)).thenReturn("puuid-failed");
    when(notificationService.dispatchPendingForPlayer(failedPlayer)).thenReturn(NotificationDispatchResult.empty());
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
  void runPollDoesNotFailPlayerWhenNotificationDeliveryFails() {
    PlayerEntity player = new PlayerEntity();
    player.setId(30L);
    player.setGameName("Bazaga");
    player.setTagLine("ESP");

    when(playerService.getActivePlayers()).thenReturn(List.of(player));
    when(pollRunService.startRun()).thenReturn(new PollRunEntity());
    when(playerService.ensurePuuid(player)).thenReturn("puuid-1");
    when(notificationService.dispatchPendingForPlayer(player))
        .thenReturn(new NotificationDispatchResult(0, 1))
        .thenReturn(NotificationDispatchResult.empty());
    when(riotClient.fetchRecentMatchIds("puuid-1")).thenReturn(List.of());

    PollSummary result = pollingService.runPoll();

    assertEquals(1, result.playersProcessed());
    assertEquals(0, result.notificationsSent());
    assertEquals(0, result.playerFailures());
    assertEquals("SUCCESS", result.status());
    verify(playerService).updateSyncSuccess(player, "puuid-1");
    verify(playerService, never()).updateSyncFailure(eq(player), anyString());
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

  @Test
  void runPollReleasesRunningFlagWhenStartRunFails() {
    when(pollRunService.startRun()).thenThrow(new IllegalStateException("Database unavailable"));

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> pollingService.runPoll());

    assertEquals("Database unavailable", exception.getMessage());
    AtomicBoolean running =
        (AtomicBoolean) ReflectionTestUtils.getField(pollingService, "running");
    assertFalse(running.get());
    verify(pollRunService, never()).failRun(any(), anyInt(), anyInt(), anyInt(), anyString());
    verifyNoInteractions(playerService, riotClient, trackedMatchService, notificationService);
  }
}
