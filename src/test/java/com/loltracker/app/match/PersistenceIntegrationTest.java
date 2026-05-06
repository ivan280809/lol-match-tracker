package com.loltracker.app.match;

import static org.junit.jupiter.api.Assertions.*;

import com.loltracker.app.player.PlayerEntity;
import com.loltracker.app.player.PlayerRepository;
import com.loltracker.lolmatchtracker.LolMatchTrackerApplication;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(
    classes = LolMatchTrackerApplication.class,
    properties = {
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false"
    })
@DirtiesContext
class PersistenceIntegrationTest {

  @Autowired private PlayerRepository playerRepository;
  @Autowired private TrackedMatchRepository trackedMatchRepository;
  @Autowired private MatchRepository matchRepository;
  @Autowired private PlayerMatchRepository playerMatchRepository;
  @Autowired private PlayerMatchService playerMatchService;
  @Autowired private TrackedMatchService trackedMatchService;

  @BeforeEach
  void setUp() {
    trackedMatchRepository.deleteAll();
    playerMatchRepository.deleteAll();
    matchRepository.deleteAll();
    playerRepository.deleteAll();
  }

  @Test
  void trackedMatchExistsByPlayerAndMatchId() {
    PlayerEntity player = new PlayerEntity();
    player.setGameName("Bazaga");
    player.setTagLine("ESP");
    player.setActive(true);
    PlayerEntity savedPlayer = playerRepository.save(player);

    TrackedMatchEntity match = new TrackedMatchEntity();
    match.setPlayer(savedPlayer);
    match.setMatchId("EUW1_555");
    match.setChampionName("Lux");
    match.setResult("VICTORY");
    match.setGameMode("CLASSIC");
    match.setDurationSeconds(1200);
    match.setGameEndAt(Instant.parse("2026-04-03T18:00:00Z"));
    match.setNotificationSent(false);
    trackedMatchRepository.save(match);

    assertTrue(trackedMatchRepository.existsByPlayerIdAndMatchId(savedPlayer.getId(), "EUW1_555"));
    assertFalse(trackedMatchRepository.existsByPlayerIdAndMatchId(savedPlayer.getId(), "EUW1_999"));
  }

  @Test
  void playerMatchKeepsOneGlobalMatchAndDedupesPlayerParticipation() {
    PlayerEntity firstPlayer = player("Bazaga", "ESP", "puuid-1");
    PlayerEntity secondPlayer = player("LuxMain", "EUW", "puuid-2");
    MatchSummary summary =
        new MatchSummary(
            "EUW1_SHARED",
            99,
            "Lux",
            true,
            "CLASSIC",
            420,
            "MIDDLE",
            "SOLO",
            8,
            2,
            11,
            210,
            13200,
            26000,
            28,
            1800,
            Instant.parse("2030-04-03T18:00:00Z"),
            "EUW1",
            "EUROPE");

    playerMatchService.record(firstPlayer, summary, false);
    playerMatchService.record(firstPlayer, summary, false);
    playerMatchService.record(secondPlayer, summary, true);

    assertEquals(1, matchRepository.count());
    assertEquals(2, playerMatchRepository.count());
    assertTrue(playerMatchRepository.existsByPlayerIdAndMatchMatchId(firstPlayer.getId(), "EUW1_SHARED"));
    assertTrue(playerMatchRepository.existsByPlayerIdAndMatchMatchId(secondPlayer.getId(), "EUW1_SHARED"));

    PlayerMatchEntity suppressed =
        playerMatchRepository.findByPlayerIdAndMatchMatchId(secondPlayer.getId(), "EUW1_SHARED").orElseThrow();
    assertTrue(suppressed.isNotificationSuppressed());
    assertEquals(MatchResult.VICTORY, suppressed.getResult());
    assertEquals(99, suppressed.getChampionId());
    assertEquals(420, matchRepository.findByMatchId("EUW1_SHARED").orElseThrow().getQueueId());
  }

  @Test
  void trackedFacadeDedupesAgainstGlobalReadModelWhenLegacyRowIsAbsent() {
    PlayerEntity player = player("Bazaga", "ESP", "puuid-dedupe");
    MatchSummary summary = rankedSoloSummary("EUW1_DEDUPE", "Lux", true);

    playerMatchService.record(player, summary, false);

    assertTrue(trackedMatchService.exists(player, "EUW1_DEDUPE"));
    assertTrue(trackedMatchService.findExisting(player, "EUW1_DEDUPE").isPresent());
    assertEquals(0, trackedMatchRepository.count());
  }

  @Test
  void globalReadPathsExposeQueueLabelsAndRecentPlayerStats() {
    PlayerEntity player = player("Bazaga", "ESP", "puuid-read");
    playerMatchService.record(player, rankedSoloSummary("EUW1_READ_1", "Lux", true), false);
    playerMatchService.record(player, aramSummary("EUW1_READ_2", "Jinx", false), false);

    List<TrackedMatchView> recent = trackedMatchService.getRecentMatchesForPlayer(player.getId(), 10);
    PlayerRecentStatsView stats = trackedMatchService.getRecentStatsForPlayer(player.getId());

    assertEquals(2, recent.size());
    assertEquals("ARAM", recent.get(0).queueLabel());
    assertEquals(MatchQueueType.ARAM, recent.get(0).queueType());
    assertEquals("Ranked Solo/Duo", recent.get(1).queueLabel());
    assertEquals(MatchQueueType.RANKED_SOLO, recent.get(1).queueType());
    assertEquals(2, stats.games());
    assertEquals(1, stats.wins());
    assertEquals(1, stats.losses());
    assertEquals("L W", stats.recentForm());
    assertEquals(List.of("Jinx", "Lux"), stats.favoriteChampions().stream().map(ChampionUsageView::championName).toList());
  }

  @Test
  void sharedMatchStatsAggregateTrackedParticipantsOnGlobalMatch() {
    PlayerEntity firstPlayer = player("Bazaga", "ESP", "puuid-shared-1");
    PlayerEntity secondPlayer = player("LuxMain", "EUW", "puuid-shared-2");

    playerMatchService.record(firstPlayer, rankedSoloSummary("EUW1_SHARED_STATS", "Lux", true), false);
    playerMatchService.record(secondPlayer, rankedSoloSummary("EUW1_SHARED_STATS", "Ahri", false), true);

    SharedMatchStatsView stats =
        trackedMatchService.getSharedMatchStats("EUW1_SHARED_STATS").orElseThrow();
    List<SharedMatchStatsView> recentShared = trackedMatchService.getSharedMatchStats(5);

    assertEquals("EUW1_SHARED_STATS", stats.matchId());
    assertEquals("Ranked Solo/Duo", stats.queueLabel());
    assertEquals(MatchQueueType.RANKED_SOLO, stats.queueType());
    assertEquals(2, stats.trackedPlayers());
    assertEquals(1, stats.wins());
    assertEquals(1, stats.losses());
    assertEquals(8.0, stats.averageKills());
    assertEquals(2.0, stats.averageDeaths());
    assertEquals(11.0, stats.averageAssists());
    assertEquals(List.of("EUW1_SHARED_STATS"), recentShared.stream().map(SharedMatchStatsView::matchId).toList());
  }

  @Test
  void historySearchUsesGlobalReadModelIncludingQueueLabels() {
    PlayerEntity rankedPlayer = player("Bazaga", "ESP", "puuid-history-1");
    PlayerEntity aramPlayer = player("LuxMain", "EUW", "puuid-history-2");
    playerMatchService.record(rankedPlayer, rankedSoloSummary("EUW1_HISTORY_1", "Lux", true), false);
    playerMatchService.record(aramPlayer, aramSummary("EUW1_HISTORY_2", "Jinx", false), false);

    assertEquals(
        Set.of(rankedPlayer.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory("solo", "", null, null));
    assertEquals(
        Set.of(aramPlayer.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory("", "jinx", null, null));
  }

  private MatchSummary rankedSoloSummary(String matchId, String championName, boolean win) {
    return new MatchSummary(
        matchId,
        99,
        championName,
        win,
        "CLASSIC",
        420,
        "MIDDLE",
        "SOLO",
        8,
        2,
        11,
        210,
        13200,
        26000,
        28,
        1800,
        Instant.parse("2030-04-03T18:00:00Z"),
        "EUW1",
        "EUROPE");
  }

  private MatchSummary aramSummary(String matchId, String championName, boolean win) {
    return new MatchSummary(
        matchId,
        222,
        championName,
        win,
        "ARAM",
        450,
        "",
        "",
        3,
        5,
        9,
        45,
        8900,
        19000,
        12,
        1200,
        Instant.parse("2030-04-03T19:00:00Z"),
        "EUW1",
        "EUROPE");
  }

  private PlayerEntity player(String gameName, String tagLine, String puuid) {
    PlayerEntity player = new PlayerEntity();
    player.setGameName(gameName);
    player.setTagLine(tagLine);
    player.setPuuid(puuid);
    player.setActive(true);
    return playerRepository.save(player);
  }
}
