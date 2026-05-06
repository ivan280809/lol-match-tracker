package com.loltracker.app.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.loltracker.app.integration.riot.RiotClient;
import com.loltracker.app.integration.riot.RiotRankEntry;
import com.loltracker.app.player.PlayerRankService.RankAvailability;
import com.loltracker.app.player.PlayerRankService.RankRefreshResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerRankServiceTest {

  @Mock private RiotClient riotClient;
  @Mock private PlayerRepository playerRepository;
  @Mock private PlayerRankRepository playerRankRepository;

  private PlayerRankService service;

  @BeforeEach
  void setUp() {
    service = new PlayerRankService(riotClient, playerRepository, playerRankRepository, java.time.Clock.systemUTC());
  }

  @Test
  void refreshRankPrefersSoloQueueAndStoresSnapshot() {
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
    PlayerEntity player = player("Bazaga", "ESP");
    player.setPuuid("puuid-1");

    when(riotClient.fetchRankEntries(RiotPlatform.EUW1, "puuid-1"))
        .thenThrow(new IllegalStateException("raw riot timeout"));

    RankRefreshResult result = service.refreshRankAvailability(player, "puuid-1");

    assertEquals(RankAvailability.REFRESH_ERROR_NO_STORED, result.availability());
    assertTrue(result.snapshot().isEmpty());
  }

  @Test
  void refreshRankAvailabilityForQueueSelectsFlexAndStoresAllQueueSnapshots() {
    PlayerEntity player = player("Bazaga", "ESP");
    player.setId(7L);
    player.setPuuid("puuid-1");
    when(playerRepository.findById(7L)).thenReturn(java.util.Optional.of(player));
    when(playerRankRepository.findAllByPlayerIdAndQueueTypeIn(eq(7L), anyCollection()))
        .thenReturn(List.of());
    when(riotClient.fetchRankEntries(RiotPlatform.EUW1, "puuid-1"))
        .thenReturn(
            List.of(
                new RiotRankEntry("RANKED_FLEX_SR", "PLATINUM", "IV", 20, 12, 8),
                new RiotRankEntry("RANKED_SOLO_5x5", "GOLD", "II", 43, 40, 35)));

    RankRefreshResult result =
        service.refreshRankAvailabilityForQueue(player, "puuid-1", PlayerRankService.FLEX_QUEUE);

    PlayerRankSnapshot snapshot = result.snapshot().orElseThrow();
    assertEquals(RankAvailability.CURRENT, result.availability());
    assertEquals("RANKED_FLEX_SR", snapshot.queueType());
    assertEquals("Flex", snapshot.displayQueueLabel());
    assertEquals("Platinum IV 20 LP", snapshot.displayName());
    assertEquals("RANKED_SOLO_5x5", player.getRankQueueType());
    assertEquals("GOLD", player.getRankTier());

    ArgumentCaptor<PlayerRankEntity> captor = ArgumentCaptor.forClass(PlayerRankEntity.class);
    verify(playerRankRepository, org.mockito.Mockito.times(2)).save(captor.capture());
    assertTrue(
        captor.getAllValues().stream()
            .anyMatch(entity -> PlayerRankService.SOLO_QUEUE.equals(entity.getQueueType())));
    assertTrue(
        captor.getAllValues().stream()
            .anyMatch(entity -> PlayerRankService.FLEX_QUEUE.equals(entity.getQueueType())));
  }

  @Test
  void refreshRankAvailabilityForQueueFallsBackOnlyToStoredMatchingQueue() {
    PlayerEntity player = player("Bazaga", "ESP");
    player.setId(7L);
    player.setPuuid("puuid-1");
    player.setRankQueueType(PlayerRankService.SOLO_QUEUE);
    player.setRankTier("GOLD");
    player.setRankDivision("II");
    player.setRankLeaguePoints(43);
    player.setRankScore(1443);
    PlayerRankEntity storedFlex = new PlayerRankEntity();
    storedFlex.setPlayer(player);
    storedFlex.setQueueType(PlayerRankService.FLEX_QUEUE);
    storedFlex.setTier("PLATINUM");
    storedFlex.setDivision("IV");
    storedFlex.setLeaguePoints(20);
    storedFlex.setScore(1620);
    when(riotClient.fetchRankEntries(RiotPlatform.EUW1, "puuid-1"))
        .thenThrow(new IllegalStateException("raw riot timeout"));
    when(playerRankRepository.findByPlayerIdAndQueueType(7L, PlayerRankService.FLEX_QUEUE))
        .thenReturn(java.util.Optional.of(storedFlex));

    RankRefreshResult result =
        service.refreshRankAvailabilityForQueue(player, "puuid-1", PlayerRankService.FLEX_QUEUE);

    assertEquals(RankAvailability.REFRESH_ERROR_STORED, result.availability());
    assertEquals("RANKED_FLEX_SR", result.snapshot().orElseThrow().queueType());
    assertEquals("Platinum IV 20 LP", result.snapshot().orElseThrow().displayName());
  }

  @Test
  void rosterAverageRankUsesStoredActivePlayerScores() {
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
