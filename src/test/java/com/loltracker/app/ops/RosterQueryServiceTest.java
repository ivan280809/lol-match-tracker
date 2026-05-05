package com.loltracker.app.ops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.loltracker.app.match.TrackedMatchService;
import com.loltracker.app.player.PlayerService;
import com.loltracker.app.player.PlayerView;
import com.loltracker.app.player.RiotPlatform;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RosterQueryServiceTest {

  @Mock private PlayerService playerService;
  @Mock private TrackedMatchService trackedMatchService;

  @InjectMocks private RosterQueryService rosterQueryService;

  @Test
  void filtersActivePlayersAndSortsByRank() {
    PlayerView ranked =
        player(1L, "Bazaga", true, null, "SUCCESS", null, RiotPlatform.EUW1, 1200, Instant.parse("2026-05-05T10:00:00Z"));
    PlayerView unranked =
        player(2L, "LuxMain", true, null, "SUCCESS", null, RiotPlatform.EUW1, null, Instant.parse("2026-05-05T11:00:00Z"));
    PlayerView archived =
        player(3L, "Old", false, Instant.parse("2026-05-05T12:00:00Z"), "ERROR", "gone", RiotPlatform.EUW1, 2000, null);
    when(playerService.getAllPlayers()).thenReturn(List.of(unranked, archived, ranked));
    when(trackedMatchService.findPlayerIdsMatchingHistory("", "", null, null)).thenReturn(Set.of());

    List<PlayerView> result =
        rosterQueryService.getRoster(
            new DashboardFilter("active", false, null, "", "", "", null, null, "rank"));

    assertEquals(List.of(ranked, unranked), result);
  }

  @Test
  void filtersByChampionHistoryAndError() {
    PlayerView errorPlayer =
        player(1L, "Bazaga", true, null, "ERROR", "Riot timeout", RiotPlatform.EUW1, null, null);
    PlayerView okPlayer =
        player(2L, "LuxMain", true, null, "SUCCESS", null, RiotPlatform.EUW1, null, null);
    when(playerService.getAllPlayers()).thenReturn(List.of(errorPlayer, okPlayer));
    when(trackedMatchService.findPlayerIdsMatchingHistory("", "Lux", null, null)).thenReturn(Set.of(1L, 2L));

    List<PlayerView> result =
        rosterQueryService.getRoster(
            new DashboardFilter("active", true, null, "", "", "Lux", null, null, "name"));

    assertEquals(List.of(errorPlayer), result);
  }

  private PlayerView player(
      Long id,
      String name,
      boolean active,
      Instant archivedAt,
      String syncStatus,
      String error,
      RiotPlatform platform,
      Integer rankScore,
      Instant lastPolledAt) {
    return new PlayerView(
        id,
        platform,
        name,
        "ESP",
        "puuid-" + id,
        active,
        archivedAt,
        lastPolledAt,
        lastPolledAt,
        syncStatus,
        error,
        rankScore == null ? null : "RANKED_SOLO_5x5",
        rankScore == null ? null : "GOLD",
        rankScore == null ? null : "IV",
        rankScore == null ? null : 10,
        rankScore,
        lastPolledAt);
  }
}
