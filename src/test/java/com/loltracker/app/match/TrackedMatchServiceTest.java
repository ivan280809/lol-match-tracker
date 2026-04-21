package com.loltracker.app.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loltracker.app.player.PlayerEntity;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackedMatchServiceTest {

  @Mock private TrackedMatchRepository trackedMatchRepository;

  @InjectMocks private TrackedMatchService trackedMatchService;

  @Test
  void createMapsSummaryIntoTrackedEntity() {
    PlayerEntity player = new PlayerEntity();
    player.setId(4L);

    MatchSummary summary =
        new MatchSummary(
            "EUW1_123", "Lux", true, "CLASSIC", 1800, Instant.parse("2026-04-03T18:00:00Z"));

    when(trackedMatchRepository.save(any(TrackedMatchEntity.class)))
        .thenAnswer(
            invocation -> {
              TrackedMatchEntity entity = invocation.getArgument(0);
              entity.setId(9L);
              return entity;
            });

    TrackedMatchEntity created = trackedMatchService.create(player, summary);

    assertEquals(9L, created.getId());
    assertEquals(player, created.getPlayer());
    assertEquals("EUW1_123", created.getMatchId());
    assertEquals("Lux", created.getChampionName());
    assertEquals("VICTORY", created.getResult());
    assertEquals("CLASSIC", created.getGameMode());
    assertEquals(1800, created.getDurationSeconds());
    assertEquals(Instant.parse("2026-04-03T18:00:00Z"), created.getGameEndAt());
    assertFalse(created.isNotificationSent());
  }

  @Test
  void markNotificationSentSetsFlagAndTimestamp() {
    TrackedMatchEntity trackedMatch = new TrackedMatchEntity();
    trackedMatch.setNotificationSent(false);

    trackedMatchService.markNotificationSent(trackedMatch);

    assertTrue(trackedMatch.isNotificationSent());
    assertNotNull(trackedMatch.getNotificationSentAt());
    verify(trackedMatchRepository).save(trackedMatch);
  }

  @Test
  void getRecentMatchesBuildsViewModels() {
    PlayerEntity player = new PlayerEntity();
    player.setId(12L);
    player.setGameName("Bazaga");
    player.setTagLine("ESP");

    TrackedMatchEntity trackedMatch = new TrackedMatchEntity();
    trackedMatch.setId(90L);
    trackedMatch.setPlayer(player);
    trackedMatch.setMatchId("EUW1_900");
    trackedMatch.setChampionName("Lux");
    trackedMatch.setResult("VICTORY");
    trackedMatch.setGameMode("CLASSIC");
    trackedMatch.setDurationSeconds(1500);
    trackedMatch.setGameEndAt(Instant.parse("2026-04-03T18:00:00Z"));
    trackedMatch.setNotificationSent(true);

    when(trackedMatchRepository.findTop20ByOrderByGameEndAtDesc()).thenReturn(List.of(trackedMatch));

    List<TrackedMatchView> views = trackedMatchService.getRecentMatches();

    assertEquals(1, views.size());
    assertEquals(90L, views.get(0).id());
    assertEquals(12L, views.get(0).playerId());
    assertEquals("Bazaga#ESP", views.get(0).playerName());
    assertEquals("EUW1_900", views.get(0).matchId());
    assertTrue(views.get(0).notificationSent());
  }
}
