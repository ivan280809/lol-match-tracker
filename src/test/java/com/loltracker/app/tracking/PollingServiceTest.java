package com.loltracker.app.tracking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;
// containsString matcher was previously used but is no longer needed after refactoring
// to use custom argument matchers in Mockito.

import com.loltracker.app.integration.riot.RiotApiException;
import com.loltracker.app.integration.riot.RiotErrorCategory;
import com.loltracker.app.match.MatchSummary;
import com.loltracker.app.match.TrackedMatchEntity;
import com.loltracker.app.match.TrackedMatchService;

import com.loltracker.app.ops.PollRunEntity;
import com.loltracker.app.ops.PollRunService;
import com.loltracker.app.ops.PollLockService;
import com.loltracker.app.ops.PollLease;
import com.loltracker.app.player.PlayerEntity;
import com.loltracker.app.player.PlayerService;
import com.loltracker.app.settings.AppConfigurationService;
import com.loltracker.app.settings.RuntimeAppConfiguration;
import com.loltracker.app.settings.RiotRegion;
import com.loltracker.app.tracking.PollSummary;
import com.loltracker.app.tracking.PollingService;
import com.loltracker.app.notification.NotificationService;
import com.loltracker.app.notification.NotificationDispatchResult;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.test.util.ReflectionTestUtils;
import com.loltracker.app.notification.NotificationDispatchResult;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.test.util.ReflectionTestUtils;
import com.loltracker.app.notification.NotificationDispatchResult;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.test.util.ReflectionTestUtils;
import com.loltracker.app.notification.NotificationDispatchResult;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.test.util.ReflectionTestUtils;

import com.loltracker.app.match.TrackedMatchService;
import com.loltracker.app.ops.OpsMetrics;
import java.time.Duration;
import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
// import java.time.Clock;  // removed duplicate import

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PollingServiceTest {

  @InjectMocks private PollingService pollingService;
  @Mock private PlayerService playerService;
  @Mock private com.loltracker.app.integration.riot.RiotMatchPort riotMatchPort;
  @Mock private com.loltracker.app.match.TrackedMatchService trackedMatchService;
  @Mock private NotificationService notificationService;
  @Mock private PollRunService pollRunService;
  @Mock private AppConfigurationService appConfigurationService;
  @Mock private Clock clock;
  @Mock private OpsMetrics opsMetrics;

  private PollRunEntity dummyRun;
  private Instant now;

  @BeforeEach
  void setUp() {
    now = Instant.now();
    dummyRun = new PollRunEntity();
    dummyRun.setId(1L);
    dummyRun.setStartedAt(now);
    RuntimeAppConfiguration cfg =
        new RuntimeAppConfiguration(
            "key", RiotRegion.EUROPE, "bot", "chat", true, false, Duration.ofMinutes(5), 10, 3);
    when(appConfigurationService.getRuntimeConfiguration()).thenReturn(cfg);
    when(pollRunService.isRateLimitPaused()).thenReturn(false);
    when(pollRunService.isScheduledRunDue(any())).thenReturn(true);
    when(pollRunService.startRun()).thenReturn(dummyRun);
    when(clock.instant()).thenReturn(now);
    // Prevent NPE in tests that do not explicitly stub this method
    when(notificationService.dispatchPendingForPlayer(any())).thenReturn(new NotificationDispatchResult(0, 0));
  }

  private PlayerEntity player(String name, String tag, Long id) {
    PlayerEntity p = new PlayerEntity();
    p.setId(id);
    p.setGameName(name);
    p.setTagLine(tag);
    p.setActive(true);
    return p;
  }
  @Test
  void runPollEnqueuesNewMatchesAndLeavesDeliveryToDispatcher() {
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
    when(riotMatchPort.fetchRecentMatchIds("puuid-1")).thenReturn(List.of("EUW1_123"));
    when(trackedMatchService.findExisting(player, "EUW1_123")).thenReturn(Optional.empty());
    when(riotMatchPort.fetchMatchSummary("EUW1_123", "puuid-1")).thenReturn(summary);
    when(trackedMatchService.create(player, summary)).thenReturn(trackedMatch);

    PollSummary result = pollingService.runPoll();

    assertEquals(1, result.playersProcessed());
    assertEquals(1, result.newMatchesFound());
    assertEquals(0, result.notificationsSent());
    assertEquals(0, result.playerFailures());
    assertEquals("SUCCESS", result.status());
    verify(notificationService).enqueueMatchNotification(trackedMatch);
    verify(notificationService, never()).dispatchPendingForPlayer(any());
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
    when(riotMatchPort.fetchRecentMatchIds("puuid-legacy")).thenReturn(List.of());

    PollSummary result = pollingService.runPoll();

    assertEquals(1, result.playersProcessed());
    assertEquals(0, result.newMatchesFound());
    assertEquals(0, result.notificationsSent());
    assertEquals(0, result.playerFailures());
    assertEquals("SUCCESS", result.status());
    verify(playerService).ensurePuuid(player);
    verify(playerService).updateSyncSuccess(player, "puuid-legacy");
    verify(notificationService, never()).dispatchPendingForPlayer(any());
  }

  @Test
  void runPollDoesNotWaitForPendingNotificationsBeforeCheckingNewMatches() {
    PlayerEntity player = new PlayerEntity();
    player.setId(12L);
    player.setGameName("Bazaga");
    player.setTagLine("ESP");

    when(playerService.getActivePlayers()).thenReturn(List.of(player));
    when(pollRunService.startRun()).thenReturn(new PollRunEntity());
    when(playerService.ensurePuuid(player)).thenReturn("puuid-1");
    when(riotMatchPort.fetchRecentMatchIds("puuid-1")).thenReturn(List.of());

    PollSummary result = pollingService.runPoll();

    assertEquals(0, result.newMatchesFound());
    assertEquals(0, result.notificationsSent());
    assertEquals(0, result.playerFailures());
    assertEquals("SUCCESS", result.status());
    verify(notificationService, never()).dispatchPendingForPlayer(any());
    verify(riotMatchPort, never()).fetchMatchSummary(anyString(), anyString());
  }

  @Test
  void runPollEnqueuesLegacyPendingNotificationsWithoutDispatchingThem() {
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
    when(riotMatchPort.fetchRecentMatchIds("puuid-1")).thenReturn(List.of());

    PollSummary result = pollingService.runPoll();

    assertEquals(0, result.notificationsSent());
    assertEquals("SUCCESS", result.status());
    verify(notificationService).enqueueMatchNotification(legacyPendingMatch);
    verify(notificationService, never()).dispatchPendingForPlayer(any());
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
    when(riotMatchPort.fetchRecentMatchIds("puuid-ok")).thenReturn(List.of());

    when(playerService.ensurePuuid(failedPlayer)).thenReturn("puuid-failed");
    when(riotMatchPort.fetchRecentMatchIds("puuid-failed"))
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
  void runPollSucceedsWithSinglePlayer() {
    PlayerEntity player = player("Bob", "TAG", 1L);
    when(playerService.getActivePlayers()).thenReturn(List.of(player));
    when(playerService.ensurePuuid(player)).thenReturn("puuid-1");
    when(trackedMatchService.findExisting(any(), eq("match-1"))).thenReturn(Optional.empty());
    when(riotMatchPort.fetchRecentMatchIds(anyString())).thenReturn(List.of("match-1"));
    when(riotMatchPort.fetchMatchSummary(eq("match-1"), anyString()))
        .thenReturn(new MatchSummary("match-1", "Lux", true, "CLASSIC", 1800, now.plusSeconds(100)));
    when(trackedMatchService.create(any(), any(), anyBoolean()))
        .thenAnswer(inv -> new TrackedMatchEntity());
    when(notificationService.dispatchPendingForPlayer(any())).thenReturn(new NotificationDispatchResult(0, 0));
    PollSummary summary = pollingService.runPoll();
    assertEquals("SUCCESS", summary.status());
    assertEquals(1, summary.playersProcessed());
    assertEquals(1, summary.newMatchesFound());
    assertEquals(0, summary.notificationsSent());
    assertEquals(0, summary.playerFailures());
    verify(playerService).updateSyncSuccess(eq(player), eq("puuid-1"));
    verify(pollRunService).completeRun(eq(dummyRun), eq(1), eq(1), eq(0), eq(List.of()));
  }


  @Test
  void runPollHandlesGenericErrorAndContinues() {
    PlayerEntity player = player("Alice", "TAG", 1L);
    when(playerService.getActivePlayers()).thenReturn(List.of(player));
    when(playerService.ensurePuuid(player)).thenReturn("puuid-1");
    when(riotMatchPort.fetchRecentMatchIds(anyString())).thenThrow(new RuntimeException("boom"));
    PollSummary summary = pollingService.runPoll();
    assertEquals("PARTIAL_SUCCESS", summary.status());
    assertEquals(1, summary.playersProcessed());
    assertEquals(0, summary.newMatchesFound());
    assertEquals(0, summary.notificationsSent());
    // errorSummary is now replaced with playerFailures
    assertEquals(1, summary.playerFailures());
    verify(playerService).updateSyncFailure(eq(player), argThat((String s) -> s != null && s.contains("boom")));
    verify(pollRunService)
        .completeRun(eq(dummyRun), eq(1), eq(0), eq(0), eq(List.of("Alice#TAG: boom")));
  }

  @Test
  void runPollDoesNotSuppressSuccessForLaterPlayers() {
    PlayerEntity p1 = player("A", "TAG", 1L);
    PlayerEntity p2 = player("B", "TAG", 2L);
    when(playerService.getActivePlayers()).thenReturn(List.of(p1, p2));
    when(playerService.ensurePuuid(p1)).thenReturn("puuid-1");
    when(playerService.ensurePuuid(p2)).thenReturn("puuid-2");
    when(riotMatchPort.fetchRecentMatchIds(anyString()))
        .thenAnswer(inv -> {
          if (inv.getArgument(0).equals("puuid-1")) {
            throw new RuntimeException("boom");
          }
          return List.of("match-1");
        });
    when(riotMatchPort.fetchMatchSummary(eq("match-1"), anyString()))
        .thenReturn(new MatchSummary("match-1", "Lux", true, "CLASSIC", 1800, now.plusSeconds(100)));
    when(trackedMatchService.create(any(), any(), anyBoolean()))
        .thenAnswer(inv -> new TrackedMatchEntity());
    when(notificationService.dispatchPendingForPlayer(any())).thenReturn(new NotificationDispatchResult(0, 0));
    PollSummary summary = pollingService.runPoll();
    assertEquals("PARTIAL_SUCCESS", summary.status());
    assertEquals(2, summary.playersProcessed());
    assertEquals(1, summary.newMatchesFound());
    assertEquals(0, summary.notificationsSent());
    // errorSummary is now replaced with playerFailures
    assertEquals(1, summary.playerFailures());
    verify(playerService).updateSyncFailure(eq(p1), argThat((String s) -> s != null && s.contains("boom")));
    verify(playerService).updateSyncSuccess(eq(p2), eq("puuid-2"));
  }

  @Test
  void runPollFailsOnRateLimitAndRecordsFailure() {
    PlayerEntity player = player("Bob", "TAG", 1L);
    when(playerService.getActivePlayers()).thenReturn(List.of(player));
    when(playerService.ensurePuuid(player)).thenReturn("puuid-1");
    when(riotMatchPort.fetchRecentMatchIds(anyString()))
        .thenThrow(new RiotApiException(RiotErrorCategory.RATE_LIMIT, "rate limited"));
    PollSummary result = pollingService.runPoll();
    assertEquals("RATE_LIMITED", result.status());
    assertEquals(1, result.playersProcessed());

    assertEquals(1, result.playerFailures());
    verify(pollRunService).rateLimited(eq(dummyRun), eq(1), eq(0), eq(0),
        argThat((Instant deadline) -> deadline.isAfter(now)), anyString());
  }

  @Test
  void runPollDoesNotDependOnNotificationDelivery() {
    PlayerEntity player = new PlayerEntity();
    player.setId(30L);
    player.setGameName("Bazaga");
    player.setTagLine("ESP");

    when(playerService.getActivePlayers()).thenReturn(List.of(player));
    when(pollRunService.startRun()).thenReturn(new PollRunEntity());
    when(playerService.ensurePuuid(player)).thenReturn("puuid-1");
    when(riotMatchPort.fetchRecentMatchIds("puuid-1")).thenReturn(List.of());

    PollSummary result = pollingService.runPoll();

    assertEquals(1, result.playersProcessed());
    assertEquals(0, result.notificationsSent());
    assertEquals(0, result.playerFailures());
    assertEquals("SUCCESS", result.status());
    verify(playerService).updateSyncSuccess(player, "puuid-1");
    verify(notificationService, never()).dispatchPendingForPlayer(any());
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
    verifyNoInteractions(playerService, riotMatchPort, trackedMatchService, notificationService, pollRunService);
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
    verifyNoInteractions(playerService, riotMatchPort, trackedMatchService, notificationService);
  }
}
