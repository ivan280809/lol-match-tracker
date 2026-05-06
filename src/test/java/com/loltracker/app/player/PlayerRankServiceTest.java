package com.loltracker.app.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.loltracker.app.integration.riot.RiotClient;
import com.loltracker.app.integration.riot.RiotRankEntry;
import com.loltracker.app.player.PlayerRankService.RankAvailability;
import com.loltracker.app.player.PlayerRankService.RankRefreshResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerRankServiceTest {

  @Mock private RiotClient riotClient;
  @Mock private PlayerRepository playerRepository;

  @Test
  void refreshRankPrefersSoloQueueAndStoresSnapshot() {
    PlayerRankService service = new PlayerRankService(riotClient, playerRepository);
    PlayerEntity player = player("Bazaga", "ESP");
    player.setPuuid("puuid-1");

    when(riotClient.fetchRankEntries(RiotPlatform.EUW1, "puuid-1"))
        .thenReturn(
            List.of(
                new RiotRankEntry("RANKED_FLEX_SR", "PLATINUM", "IV", 20, 12, 8),
                new RiotRankEntry("RANKED_SOLO_5x5", "GOLD", "II", 43, 40, 35)));

    RankRefreshResult result = service.refreshRankAvailability(player, "puuid-1");
    PlayerRankSnapshot snapshot = result.snapshot().orElseThrow();

    assertEquals(RankAvailability.CURRENT, result.availability());
    assertEquals("Gold II 43 LP", snapshot.displayName());
    assertEquals(1443, snapshot.score());
    assertEquals("RANKED_SOLO_5x5", player.getRankQueueType());
    assertEquals("GOLD", player.getRankTier());
    assertEquals("II", player.getRankDivision());
    assertEquals(43, player.getRankLeaguePoints());
    assertEquals(1443, player.getRankScore());
    verify(playerRepository).save(player);
  }

  @Test
  void refreshRankAvailabilityReportsUnrankedAndClearsStoredRank() {
    PlayerRankService service = new PlayerRankService(riotClient, playerRepository);
    PlayerEntity player = player("Bazaga", "ESP");
    player.setPuuid("puuid-1");
    player.setRankQueueType("RANKED_SOLO_5x5");
    player.setRankTier("GOLD");
    player.setRankDivision("II");
    player.setRankLeaguePoints(43);
    player.setRankScore(1443);

    when(riotClient.fetchRankEntries(RiotPlatform.EUW1, "puuid-1"))
        .thenReturn(List.of(new RiotRankEntry("CHERRY", "GOLD", "II", 43, 40, 35)));

    RankRefreshResult result = service.refreshRankAvailability(player, "puuid-1");

    assertEquals(RankAvailability.UNRANKED, result.availability());
    assertTrue(result.snapshot().isEmpty());
    assertNull(player.getRankQueueType());
    assertNull(player.getRankTier());
    assertNull(player.getRankDivision());
    assertNull(player.getRankLeaguePoints());
    assertNull(player.getRankScore());
    verify(playerRepository).save(player);
  }

  @Test
  void refreshRankAvailabilityReportsStoredRankWhenPuuidIsMissing() {
    PlayerRankService service = new PlayerRankService(riotClient, playerRepository);
    PlayerEntity player = player("Bazaga", "ESP");
    player.setRankQueueType("RANKED_SOLO_5x5");
    player.setRankTier("SILVER");
    player.setRankDivision("I");
    player.setRankLeaguePoints(78);
    player.setRankScore(1178);

    RankRefreshResult result = service.refreshRankAvailability(player, " ");

    assertEquals(RankAvailability.STORED, result.availability());
    assertEquals("Silver I 78 LP", result.snapshot().orElseThrow().displayName());
    verifyNoInteractions(riotClient, playerRepository);
  }

  @Test
  void refreshRankAvailabilityReportsRefreshErrorWithStoredRank() {
    PlayerRankService service = new PlayerRankService(riotClient, playerRepository);
    PlayerEntity player = player("Bazaga", "ESP");
    player.setPuuid("puuid-1");
    player.setRankQueueType("RANKED_SOLO_5x5");
    player.setRankTier("GOLD");
    player.setRankDivision("IV");
    player.setRankLeaguePoints(10);
    player.setRankScore(1210);

    when(riotClient.fetchRankEntries(RiotPlatform.EUW1, "puuid-1"))
        .thenThrow(new IllegalStateException("raw riot timeout"));

    RankRefreshResult result = service.refreshRankAvailability(player, "puuid-1");

    assertEquals(RankAvailability.REFRESH_ERROR_STORED, result.availability());
    assertEquals("Gold IV 10 LP", result.snapshot().orElseThrow().displayName());
  }

  @Test
  void refreshRankAvailabilityReportsRefreshErrorWithoutStoredRank() {
    PlayerRankService service = new PlayerRankService(riotClient, playerRepository);
    PlayerEntity player = player("Bazaga", "ESP");
    player.setPuuid("puuid-1");

    when(riotClient.fetchRankEntries(RiotPlatform.EUW1, "puuid-1"))
        .thenThrow(new IllegalStateException("raw riot timeout"));

    RankRefreshResult result = service.refreshRankAvailability(player, "puuid-1");

    assertEquals(RankAvailability.REFRESH_ERROR_NO_STORED, result.availability());
    assertTrue(result.snapshot().isEmpty());
  }

  @Test
  void rosterAverageRankUsesStoredActivePlayerScores() {
    PlayerRankService service = new PlayerRankService(riotClient, playerRepository);
    PlayerEntity gold = player("Gold", "EUW");
    gold.setRankScore(1443);
    PlayerEntity silver = player("Silver", "EUW");
    silver.setRankScore(1178);

    when(playerRepository.findAllByActiveTrueAndArchivedAtIsNullOrderByGameNameAsc())
        .thenReturn(List.of(gold, silver));

    PlayerRankSnapshot average = service.rosterAverageRank().orElseThrow();

    assertEquals("Gold III 11 LP", average.displayName());
    assertEquals(1311, average.score());
  }

  @Test
  void rosterAverageRankIsEmptyWhenNoRankSnapshotsExist() {
    PlayerRankService service = new PlayerRankService(riotClient, playerRepository);
    when(playerRepository.findAllByActiveTrueAndArchivedAtIsNullOrderByGameNameAsc())
        .thenReturn(List.of(player("Unranked", "EUW")));

    assertTrue(service.rosterAverageRank().isEmpty());
  }

  private PlayerEntity player(String gameName, String tagLine) {
    PlayerEntity player = new PlayerEntity();
    player.setGameName(gameName);
    player.setTagLine(tagLine);
    player.setPlatform(RiotPlatform.EUW1);
    player.setActive(true);
    return player;
  }
}
