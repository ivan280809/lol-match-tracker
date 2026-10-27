package com.loltracker.app.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.loltracker.app.integration.riot.RiotClient;
import com.loltracker.app.integration.riot.RiotRankEntry;
import com.loltracker.app.player.PlayerRankService.RankAvailability;
import com.loltracker.app.player.PlayerRankService.RankRefreshResult;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
  void refreshRankAvailabilityForQueueFallsBackOnlyToStoredMatchingQueue_ExistingQueue() {
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
  void notificationRankUsesFreshQueueSnapshotWithoutCallingRiot() {
    Instant now = Instant.parse("2026-10-05T00:00:00Z");
    PlayerEntity player = player("Bazaga", "ESP");
    player.setId(7L);
    player.setPuuid("puuid-1");
    PlayerRankEntity stored = rankEntry(player, PlayerRankService.SOLO_QUEUE, now.minusSeconds(60));
    service =
        new PlayerRankService(
            riotClient,
            playerRepository,
            playerRankRepository,
            Clock.fixed(now, ZoneOffset.UTC));
    ReflectionTestUtils.setField(service, "rankCacheTtl", Duration.ofMinutes(15));
    when(playerRankRepository.findByPlayerIdAndQueueType(7L, PlayerRankService.SOLO_QUEUE))
        .thenReturn(Optional.of(stored));

    RankRefreshResult result =
        service.rankForNotification(player, "puuid-1", PlayerRankService.SOLO_QUEUE);

    assertEquals(RankAvailability.STORED, result.availability());
    assertEquals(stored.getUpdatedAt(), result.updatedAt());
    assertEquals("GOLD", result.snapshot().orElseThrow().tier());
    verifyNoInteractions(riotClient);
  }

  @Test
  void notificationRankRefreshesExpiredQueueSnapshot() {
    Instant now = Instant.parse("2026-10-05T00:00:00Z");
    PlayerEntity player = player("Bazaga", "ESP");
    player.setId(7L);
    player.setPuuid("puuid-1");
    PlayerRankEntity stale = rankEntry(player, PlayerRankService.SOLO_QUEUE, now.minus(Duration.ofHours(1)));
    service =
        new PlayerRankService(
            riotClient,
            playerRepository,
            playerRankRepository,
            Clock.fixed(now, ZoneOffset.UTC));
    ReflectionTestUtils.setField(service, "rankCacheTtl", Duration.ofMinutes(15));
    when(playerRankRepository.findByPlayerIdAndQueueType(7L, PlayerRankService.SOLO_QUEUE))
        .thenReturn(Optional.of(stale));
    when(playerRankRepository.findAllByPlayerIdAndQueueTypeIn(eq(7L), anyCollection()))
        .thenReturn(List.of(stale));
    when(playerRepository.findById(7L)).thenReturn(Optional.of(player));
    when(riotClient.fetchRankEntries(RiotPlatform.EUW1, "puuid-1"))
        .thenReturn(List.of(new RiotRankEntry("RANKED_SOLO_5x5", "PLATINUM", "IV", 20, 12, 8)));

    RankRefreshResult result =
        service.rankForNotification(player, "puuid-1", PlayerRankService.SOLO_QUEUE);

    assertEquals(RankAvailability.CURRENT, result.availability());
    assertEquals("PLATINUM", result.snapshot().orElseThrow().tier());
    verify(riotClient).fetchRankEntries(RiotPlatform.EUW1, "puuid-1");
  }

  @Test
  void expiredQueueRefreshFailureUsesTheSingleLoadedStaleSnapshotAndTimestamp() {
    Instant now = Instant.parse("2026-10-05T00:00:00Z");
    Instant updatedAt = now.minus(Duration.ofHours(1));
    PlayerEntity player = player("Bazaga", "ESP");
    player.setId(7L);
    player.setPuuid("puuid-1");
    PlayerRankEntity stale = rankEntry(player, PlayerRankService.SOLO_QUEUE, updatedAt);
    service = new PlayerRankService(riotClient, playerRepository, playerRankRepository, Clock.fixed(now, ZoneOffset.UTC));
    ReflectionTestUtils.setField(service, "rankCacheTtl", Duration.ofMinutes(15));
    when(playerRankRepository.findByPlayerIdAndQueueType(7L, PlayerRankService.SOLO_QUEUE))
        .thenReturn(Optional.of(stale));
    when(riotClient.fetchRankEntries(RiotPlatform.EUW1, "puuid-1"))
        .thenThrow(new IllegalStateException("fixture outage"));

    RankRefreshResult result = service.rankForNotification(player, "puuid-1", PlayerRankService.SOLO_QUEUE);

    assertEquals(RankAvailability.REFRESH_ERROR_STORED, result.availability());
    assertEquals(updatedAt, result.updatedAt());
    assertEquals("GOLD", result.snapshot().orElseThrow().tier());
    verify(playerRankRepository, times(1)).findByPlayerIdAndQueueType(7L, PlayerRankService.SOLO_QUEUE);
  }

  @Test
  void confirmedUnrankedQueueIsReusedUntilItsPersistedTtlExpires() {
    Instant now = Instant.parse("2026-10-05T00:00:00Z");
    PlayerEntity player = player("Bazaga", "ESP");
    player.setId(7L);
    player.setPuuid("puuid-1");
    Map<String, PlayerRankEntity> snapshots = new HashMap<>();
    service = new PlayerRankService(riotClient, playerRepository, playerRankRepository, Clock.fixed(now, ZoneOffset.UTC));
    ReflectionTestUtils.setField(service, "rankCacheTtl", Duration.ofMinutes(15));
    when(playerRankRepository.findByPlayerIdAndQueueType(eq(7L), any()))
        .thenAnswer(call -> Optional.ofNullable(snapshots.get(call.getArgument(1))));
    when(playerRankRepository.findAllByPlayerIdAndQueueTypeIn(eq(7L), anyCollection()))
        .thenAnswer(call -> List.copyOf(snapshots.values()));
    when(playerRankRepository.save(any(PlayerRankEntity.class)))
        .thenAnswer(call -> {
          PlayerRankEntity entity = call.getArgument(0);
          snapshots.put(entity.getQueueType(), entity);
          return entity;
        });
    when(playerRepository.findById(7L)).thenReturn(Optional.of(player));
    when(riotClient.fetchRankEntries(RiotPlatform.EUW1, "puuid-1")).thenReturn(List.of());

    RankRefreshResult first = service.rankForNotification(player, "puuid-1", PlayerRankService.SOLO_QUEUE);
    RankRefreshResult second = service.rankForNotification(player, "puuid-1", PlayerRankService.SOLO_QUEUE);

    assertEquals(RankAvailability.UNRANKED, first.availability());
    assertEquals(RankAvailability.UNRANKED, second.availability());
    assertEquals(now, second.updatedAt());
    verify(riotClient, times(1)).fetchRankEntries(RiotPlatform.EUW1, "puuid-1");

    PlayerRankService expiredService = new PlayerRankService(
        riotClient, playerRepository, playerRankRepository,
        Clock.fixed(now.plus(Duration.ofMinutes(16)), ZoneOffset.UTC));
    ReflectionTestUtils.setField(expiredService, "rankCacheTtl", Duration.ofMinutes(15));
    expiredService.rankForNotification(player, "puuid-1", PlayerRankService.SOLO_QUEUE);
    verify(riotClient, times(2)).fetchRankEntries(RiotPlatform.EUW1, "puuid-1");
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

  /**
   * Verify that when queueType is {@code null} and the player has a fresh {@code rankUpdatedAt}
   * but no stored rank, the service treats the last Riot response (which may have been an empty
   * list) as an unranked result and preserves the timestamp. Subsequent calls within the TTL use
   * the cached timestamp and do not hit Riot again.
   */
  @Test
  void confirmedUnrankedWithNullQueueIsCachedUntilTTLExpires() {
    Instant now = Instant.parse("2026-10-05T00:00:00Z");
    PlayerEntity player = player("Bazaga", "ESP");
    player.setId(7L);
    player.setPuuid("puuid-1");
    player.setRankUpdatedAt(now); // fresh timestamp, no stored rank fields

    // service with null repository to skip DB lookups for stored rank
    PlayerRankService noRepoService = new PlayerRankService(
        riotClient, playerRepository, null, Clock.fixed(now, ZoneOffset.UTC));
    ReflectionTestUtils.setField(noRepoService, "rankCacheTtl", Duration.ofMinutes(15));

    // First call: should return UNRANKED with timestamp and NOT call Riot
    RankRefreshResult first = noRepoService.rankForNotification(player, "puuid-1", null);
    assertEquals(RankAvailability.UNRANKED, first.availability());
    assertEquals(now, first.updatedAt());
    verifyNoInteractions(riotClient);

    // Advance clock beyond TTL and prepare a fresh service that will invoke Riot
    PlayerRankService freshService = new PlayerRankService(
        riotClient, playerRepository, null, Clock.fixed(now.plus(Duration.ofMinutes(16)), ZoneOffset.UTC));
    ReflectionTestUtils.setField(freshService, "rankCacheTtl", Duration.ofMinutes(15));
    when(riotClient.fetchRankEntries(RiotPlatform.EUW1, "puuid-1")).thenReturn(List.of());

    RankRefreshResult second = freshService.rankForNotification(player, "puuid-1", null);
    // After refresh, updatedAt should reflect new timestamp (now + 16 min)
    assertEquals(RankAvailability.UNRANKED, second.availability());
    assertEquals(now.plus(Duration.ofMinutes(16)), second.updatedAt());
    verify(riotClient, times(1)).fetchRankEntries(RiotPlatform.EUW1, "puuid-1");
  }

  private PlayerEntity player(String gameName, String tagLine) {
    PlayerEntity player = new PlayerEntity();
    player.setGameName(gameName);
    player.setTagLine(tagLine);
    player.setPlatform(RiotPlatform.EUW1);
    player.setActive(true);
    return player;
  }

  private PlayerRankEntity rankEntry(PlayerEntity player, String queueType, Instant updatedAt) {
    PlayerRankEntity entity = new PlayerRankEntity();
    entity.setPlayer(player);
    entity.setQueueType(queueType);
    entity.setTier("GOLD");
    entity.setDivision("IV");
    entity.setLeaguePoints(10);
    entity.setScore(1210);
    entity.setUpdatedAt(updatedAt);
    return entity;
  }
}
