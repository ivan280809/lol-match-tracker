package com.loltracker.app.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.loltracker.app.match.TrackedMatchEntity;
import com.loltracker.app.match.TrackedMatchRepository;
import com.loltracker.app.player.PlayerEntity;
import com.loltracker.app.player.PlayerRankService;
import com.loltracker.app.player.PlayerRankSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationStatsServiceTest {

  @Mock private TrackedMatchRepository trackedMatchRepository;
  @Mock private PlayerRankService playerRankService;

  @Test
  void buildForCalculatesLocalMatchStats() {
    NotificationStatsService service = new NotificationStatsService(trackedMatchRepository, playerRankService);
    PlayerEntity player = player();
    TrackedMatchEntity current =
        match(player, "VICTORY", "Lux", 1800, "2026-04-03T18:00:00Z");
    when(playerRankService.refreshRank(player, "puuid-1"))
        .thenReturn(Optional.of(new PlayerRankSnapshot("RANKED_SOLO_5x5", "GOLD", "II", 43, 1443)));
    when(playerRankService.rosterAverageRank())
        .thenReturn(Optional.of(new PlayerRankSnapshot("ROSTER_AVERAGE", "SILVER", "I", 78, 1178)));

    when(trackedMatchRepository.findTop10ByPlayerIdOrderByGameEndAtDesc(7L))
        .thenReturn(
            List.of(
                current,
                match(player, "VICTORY", "Ahri", 1200, "2026-04-02T18:00:00Z"),
                match(player, "DEFEAT", "Lux", 900, "2026-04-01T18:00:00Z")));
    when(trackedMatchRepository.findTop20ByPlayerIdAndChampionNameIgnoreCaseOrderByGameEndAtDesc(
            7L, "Lux"))
        .thenReturn(
            List.of(
                current,
                match(player, "DEFEAT", "Lux", 1500, "2026-04-02T18:00:00Z"),
                match(player, "VICTORY", "Lux", 2100, "2026-04-01T18:00:00Z")));
    when(trackedMatchRepository
            .findAllByPlayerIdAndGameEndAtGreaterThanEqualAndGameEndAtLessThanOrderByGameEndAtDesc(
                7L, Instant.parse("2026-04-02T22:00:00Z"), Instant.parse("2026-04-03T22:00:00Z")))
        .thenReturn(
            List.of(
                current,
                match(player, "DEFEAT", "Ahri", 1200, "2026-04-03T16:00:00Z"),
                match(player, "VICTORY", "Lux", 900, "2026-04-03T15:00:00Z")));

    NotificationStatsSnapshot stats = service.buildFor(current);

    assertEquals("W W L", stats.recentForm());
    assertEquals(2, stats.currentStreakCount());
    assertEquals("victoria", stats.currentStreakResult());
    assertEquals(2, stats.dayWins());
    assertEquals(1, stats.dayLosses());
    assertEquals(2, stats.championWins());
    assertEquals(1, stats.championLosses());
    assertEquals(67, stats.championWinRate());
    assertEquals(1300, stats.recentAverageDurationSeconds());
    assertEquals("Gold II 43 LP", stats.playerRank());
    assertEquals("Silver I 78 LP", stats.rosterAverageRank());
    assertEquals(265, stats.rankDelta());
  }

  private PlayerEntity player() {
    PlayerEntity player = new PlayerEntity();
    player.setId(7L);
    player.setGameName("Bazaga");
    player.setTagLine("ESP");
    player.setPuuid("puuid-1");
    return player;
  }

  private TrackedMatchEntity match(
      PlayerEntity player, String result, String champion, long durationSeconds, String gameEndAt) {
    TrackedMatchEntity match = new TrackedMatchEntity();
    match.setPlayer(player);
    match.setResult(result);
    match.setChampionName(champion);
    match.setGameMode("CLASSIC");
    match.setDurationSeconds(durationSeconds);
    match.setGameEndAt(Instant.parse(gameEndAt));
    return match;
  }
}
