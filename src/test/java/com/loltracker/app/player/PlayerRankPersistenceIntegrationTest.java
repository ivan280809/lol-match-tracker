package com.loltracker.app.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loltracker.app.integration.riot.RiotAccountPort;
import com.loltracker.app.integration.riot.RiotMatchPort;
import com.loltracker.app.integration.riot.RiotRankEntry;
import com.loltracker.app.integration.riot.RiotRankPort;
import com.loltracker.app.integration.telegram.TelegramNotificationPort;
import com.loltracker.lolmatchtracker.LolMatchTrackerApplication;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(
    classes = {LolMatchTrackerApplication.class, PlayerRankPersistenceIntegrationTest.TestPorts.class},
    properties = {
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false",
      "spring.jpa.properties.hibernate.generate_statistics=true",
      "app.poll.initial-delay=PT876000H",
      "app.poll.scheduler-tick=PT876000H"
    })
@DirtiesContext
class PlayerRankPersistenceIntegrationTest {

  @Autowired private PlayerRepository playerRepository;
  @Autowired private PlayerRankRepository playerRankRepository;
  @Autowired private PlayerRankService playerRankService;
  @Autowired private SessionFactory sessionFactory;
  @Autowired private RiotRankPort riotRankPort;
  @Autowired private MutableClock testClock;

  @TestConfiguration
  static class TestPorts {
    @Bean
    @Primary
    RiotRankPort fakeRiotRankPort() {
      return mock(RiotRankPort.class);
    }

    @Bean
    @Primary
    RiotAccountPort fakeRiotAccountPort() {
      return mock(RiotAccountPort.class);
    }

    @Bean
    @Primary
    RiotMatchPort fakeRiotMatchPort() {
      return mock(RiotMatchPort.class);
    }

    @Bean
    @Primary
    TelegramNotificationPort fakeTelegramNotificationPort() {
      return mock(TelegramNotificationPort.class);
    }

    @Bean
    @Primary
    MutableClock fixedTestClock() {
      return new MutableClock(Instant.parse("2026-10-05T00:00:00Z"));
    }
  }

  static class MutableClock extends Clock {
    private final AtomicReference<Instant> current;

    MutableClock(Instant initial) {
      current = new AtomicReference<>(initial);
    }

    void advance(Duration duration) {
      current.updateAndGet(value -> value.plus(duration));
    }

    void reset() {
      current.set(Instant.parse("2026-10-05T00:00:00Z"));
    }

    @Override
    public Instant instant() {
      return current.get();
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }
  }

  @BeforeEach
  void cleanDatabase() {
    testClock.reset();
    ReflectionTestUtils.setField(playerRankService, "rankCacheTtl", Duration.ofMinutes(15));
    playerRankRepository.deleteAll();
    playerRepository.deleteAll();
  }

  @Test
  void freshQueueRankUsesOneSqlReadWithoutCallingRiot() {
    PlayerEntity player = new PlayerEntity();
    player.setGameName("Rank Cache Fixture");
    player.setTagLine("TEST");
    player.setPuuid("fake-puuid-rank-cache");
    player.setActive(true);
    player = playerRepository.saveAndFlush(player);

    PlayerRankEntity rank = new PlayerRankEntity();
    rank.setPlayer(player);
    rank.setQueueType(PlayerRankService.SOLO_QUEUE);
    rank.setTier("GOLD");
    rank.setDivision("II");
    rank.setLeaguePoints(42);
    rank.setScore(10_042);
    rank.setUpdatedAt(Instant.parse("2026-10-05T00:00:00Z"));
    playerRankRepository.saveAndFlush(rank);

    Statistics statistics = sessionFactory.getStatistics();
    for (int warmup = 0; warmup < 2; warmup++) {
      playerRankService.rankForNotification(player, player.getPuuid(), PlayerRankService.SOLO_QUEUE);
    }
    List<Long> samplesMicros = new ArrayList<>(12);
    long totalStatements = 0;
    for (int sample = 0; sample < 12; sample++) {
      statistics.clear();
      long started = System.nanoTime();
      PlayerRankService.RankRefreshResult result =
          playerRankService.rankForNotification(player, player.getPuuid(), PlayerRankService.SOLO_QUEUE);
      samplesMicros.add((System.nanoTime() - started) / 1_000);
      assertEquals(PlayerRankService.RankAvailability.STORED, result.availability());
      assertEquals("GOLD", result.snapshot().orElseThrow().tier());
      long sqlStatements = statistics.getPrepareStatementCount();
      assertEquals(1, sqlStatements, "A fresh persisted queue snapshot should require one SQL lookup");
      totalStatements += sqlStatements;
    }
    Collections.sort(samplesMicros);
    long p95Micros = samplesMicros.get((int) Math.ceil(samplesMicros.size() * 0.95) - 1);
    System.out.printf(
        java.util.Locale.ROOT,
        "RANK_PERSISTENCE_BENCHMARK queue=%s samples=%d sql_statements=%d statements_per_lookup=%.1f p95_us=%d%n",
        PlayerRankService.SOLO_QUEUE, samplesMicros.size(), totalStatements,
        (double) totalStatements / samplesMicros.size(), p95Micros);
  }

  @Test
  void explicitRefreshBypassesFreshQueueTtlAndPersistsUpdatedSnapshot() {
    PlayerEntity player = new PlayerEntity();
    player.setGameName("Explicit Refresh Fixture");
    player.setTagLine("TEST");
    player.setPuuid("fake-puuid-explicit-refresh");
    player.setActive(true);
    player = playerRepository.saveAndFlush(player);

    Instant previousUpdate = Instant.parse("2026-10-04T23:59:00Z");
    PlayerRankEntity fresh = new PlayerRankEntity();
    fresh.setPlayer(player);
    fresh.setQueueType(PlayerRankService.SOLO_QUEUE);
    fresh.setTier("GOLD");
    fresh.setDivision("II");
    fresh.setLeaguePoints(42);
    fresh.setScore(10_042);
    fresh.setUpdatedAt(previousUpdate);
    playerRankRepository.saveAndFlush(fresh);

    when(riotRankPort.fetchRankEntries(any(), eq(player.getPuuid())))
        .thenReturn(List.of(new RiotRankEntry(PlayerRankService.SOLO_QUEUE, "PLATINUM", "IV", 20, 12, 8)));

    PlayerRankService.RankRefreshResult result =
        playerRankService.refreshRankAvailabilityForQueue(player, player.getPuuid(), PlayerRankService.SOLO_QUEUE);

    assertEquals(PlayerRankService.RankAvailability.CURRENT, result.availability());
    assertEquals("PLATINUM", result.snapshot().orElseThrow().tier());
    verify(riotRankPort).fetchRankEntries(any(), eq(player.getPuuid()));
    PlayerRankEntity updated = playerRankRepository
        .findByPlayerIdAndQueueType(player.getId(), PlayerRankService.SOLO_QUEUE)
        .orElseThrow();
    assertEquals("PLATINUM", updated.getTier());
    assertTrue(updated.getUpdatedAt().isAfter(previousUpdate));
  }

  @Test
  void confirmedUnrankedResultIsCachedWithTimestampUntilTtlExpires() {
    ReflectionTestUtils.setField(playerRankService, "rankCacheTtl", Duration.ofMinutes(5));
    PlayerEntity player = new PlayerEntity();
    player.setGameName("Unranked Cache Fixture");
    player.setTagLine("TEST");
    player.setPuuid("fake-puuid-unranked-cache");
    player.setActive(true);
    player = playerRepository.saveAndFlush(player);
    when(riotRankPort.fetchRankEntries(any(), eq(player.getPuuid()))).thenReturn(List.of());

    PlayerRankService.RankRefreshResult first =
        playerRankService.rankForNotification(player, player.getPuuid(), null);
    assertEquals(PlayerRankService.RankAvailability.UNRANKED, first.availability());
    assertEquals(testClock.instant(), first.updatedAt());

    PlayerEntity reloaded = playerRepository.findById(player.getId()).orElseThrow();
    PlayerRankService.RankRefreshResult second =
        playerRankService.rankForNotification(reloaded, reloaded.getPuuid(), null);
    assertEquals(PlayerRankService.RankAvailability.UNRANKED, second.availability());
    assertEquals(first.updatedAt(), second.updatedAt());
    verify(riotRankPort).fetchRankEntries(any(), eq(player.getPuuid()));

    testClock.advance(Duration.ofMinutes(6));
    PlayerEntity expired = playerRepository.findById(player.getId()).orElseThrow();
    PlayerRankService.RankRefreshResult refreshed =
        playerRankService.rankForNotification(expired, expired.getPuuid(), null);
    assertEquals(PlayerRankService.RankAvailability.UNRANKED, refreshed.availability());
    assertEquals(testClock.instant(), refreshed.updatedAt());
    assertTrue(refreshed.updatedAt().isAfter(first.updatedAt()));
    verify(riotRankPort, org.mockito.Mockito.times(2)).fetchRankEntries(any(), eq(player.getPuuid()));
  }

  /**
   * Regression test for issue #5. After a stale queue snapshot is refreshed, the supplied
   * {@link PlayerEntity} must be updated so that subsequent notifications using a null
   * queue type return the refreshed primary rank.
   */
  @Test
  void staleQueueSnapshotRefreshUpdatesDetachedPlayer() {
    // Create a player with a primary GOLD rank
    PlayerEntity player = new PlayerEntity();
    player.setGameName("Stale Queue Fixture");
    player.setTagLine("TEST");
    player.setPuuid("fake-puuid-stale-queue");
    player.setActive(true);
    player.setRankQueueType(null);
    player.setRankTier("GOLD");
    player.setRankDivision("II");
    player.setRankLeaguePoints(42);
    player.setRankScore(10_042);
    player.setRankUpdatedAt(testClock.instant());
    player = playerRepository.saveAndFlush(player);

    // Stale solo snapshot that should be refreshed to PLATINUM
    PlayerRankEntity soloSnapshot = new PlayerRankEntity();
    soloSnapshot.setPlayer(player);
    soloSnapshot.setQueueType(PlayerRankService.SOLO_QUEUE);
    soloSnapshot.setTier("GOLD");
    soloSnapshot.setDivision("II");
    soloSnapshot.setLeaguePoints(42);
    soloSnapshot.setScore(10_042);
    soloSnapshot.setUpdatedAt(testClock.instant().minus(Duration.ofMinutes(30)));
    playerRankRepository.saveAndFlush(soloSnapshot);

    // Mock Riot to return PLATINUM for solo
    when(riotRankPort.fetchRankEntries(any(), eq(player.getPuuid())))
        .thenReturn(List.of(new RiotRankEntry(PlayerRankService.SOLO_QUEUE, "PLATINUM", "IV", 20, 12, 8)));

    // First call with queue type SOLO – should trigger refresh
    PlayerRankService.RankRefreshResult soloResult =
        playerRankService.rankForNotification(player, player.getPuuid(), PlayerRankService.SOLO_QUEUE);
    assertEquals(PlayerRankService.RankAvailability.CURRENT, soloResult.availability());
    assertEquals("PLATINUM", soloResult.snapshot().orElseThrow().tier());
    // After refresh, solo snapshot should be updated
    PlayerRankEntity refreshedSolo =
        playerRankRepository
            .findByPlayerIdAndQueueType(player.getId(), PlayerRankService.SOLO_QUEUE)
            .orElseThrow();
    assertEquals("PLATINUM", refreshedSolo.getTier());
    assertTrue(refreshedSolo.getUpdatedAt().isAfter(soloSnapshot.getUpdatedAt()));
    // Primary fields of the same PlayerEntity should also be updated
    assertEquals("PLATINUM", player.getRankTier());
    assertEquals("IV", player.getRankDivision());
    assertEquals(20, player.getRankLeaguePoints());
    assertEquals(1_620, player.getRankScore());
    assertEquals(testClock.instant(), player.getRankUpdatedAt());

    // Second call with queue type null – should use updated primary rank without extra Riot call
    PlayerRankService.RankRefreshResult defaultResult =
        playerRankService.rankForNotification(player, player.getPuuid(), null);
    assertEquals(PlayerRankService.RankAvailability.STORED, defaultResult.availability());
    assertEquals("PLATINUM", defaultResult.snapshot().orElseThrow().tier());
    // Verify Riot called only once
    verify(riotRankPort, org.mockito.Mockito.times(1)).fetchRankEntries(any(), eq(player.getPuuid()));
  }
}
