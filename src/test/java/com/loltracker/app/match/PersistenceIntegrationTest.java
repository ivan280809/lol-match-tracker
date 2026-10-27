package com.loltracker.app.match;

import static org.junit.jupiter.api.Assertions.*;

import com.loltracker.app.player.PlayerEntity;
import com.loltracker.app.player.PlayerRepository;
import com.loltracker.lolmatchtracker.LolMatchTrackerApplication;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(
    classes = LolMatchTrackerApplication.class,
    properties = {
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false",
      "spring.jpa.properties.hibernate.generate_statistics=true"
    })
@DirtiesContext
class PersistenceIntegrationTest {

  @Autowired private PlayerRepository playerRepository;
  @Autowired private TrackedMatchRepository trackedMatchRepository;
  @Autowired private MatchRepository matchRepository;
  @Autowired private PlayerMatchRepository playerMatchRepository;
  @Autowired private PlayerMatchService playerMatchService;
  @Autowired private TrackedMatchService trackedMatchService;
  @Autowired private SessionFactory sessionFactory;

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
  void batchExistingMatchLookupCombinesLegacyAndGlobalRows() {
    PlayerEntity player = player("Bazaga", "ESP", "puuid-batch-dedupe");
    playerMatchService.record(player, rankedSoloSummary("EUW1_GLOBAL_BATCH", "Lux", true), false);
    legacyMatch(player, "EUW1_LEGACY_BATCH", "Jinx", "CLASSIC", 420, Instant.parse("2030-04-03T18:00:00Z"));

    assertEquals(
        Set.of("EUW1_GLOBAL_BATCH", "EUW1_LEGACY_BATCH"),
        trackedMatchService.findExistingMatchIds(
            player, List.of("EUW1_GLOBAL_BATCH", "EUW1_LEGACY_BATCH", "EUW1_NEW")));
  }

  @Test
  void batchLookupReducesQueriesForOneHundredPlayersAndSamePage() {
    List<PlayerEntity> players = new ArrayList<>();
    List<String> pageIds = new ArrayList<>();
    for (int match = 0; match < 20; match++) pageIds.add("EUW1_BATCH_MEASURE_%02d".formatted(match));
    for (int playerIndex = 0; playerIndex < 100; playerIndex++) {
      PlayerEntity player = player("batch-player-" + playerIndex, "EUW", "puuid-batch-measure-" + playerIndex);
      players.add(player);
      for (String matchId : pageIds) {
        legacyMatch(player, matchId, "Lux", "CLASSIC", 420, Instant.parse("2030-04-03T18:00:00Z"));
      }
    }

    Statistics statistics = sessionFactory.getStatistics();
    Map<Long, Set<String>> baselineResults = new HashMap<>();
    statistics.clear();
    for (PlayerEntity player : players) {
      Set<String> existing = new java.util.HashSet<>();
      for (String matchId : pageIds) {
        if (trackedMatchService.findExisting(player, matchId).isPresent()) existing.add(matchId);
      }
      baselineResults.put(player.getId(), existing);
    }
    long baselineQueries = statistics.getPrepareStatementCount();

    Map<Long, Set<String>> batchedResults = new HashMap<>();
    statistics.clear();
    for (PlayerEntity player : players) {
      batchedResults.put(player.getId(), trackedMatchService.findExistingMatchIds(player, pageIds));
    }
    long batchedQueries = statistics.getPrepareStatementCount();

    assertEquals(baselineResults, batchedResults);
    assertEquals(2_000, baselineQueries, "The executed baseline performs one tracked-row lookup per ID");
    assertEquals(200, batchedQueries, "The executed batch performs one legacy and one global lookup per player page");
    System.out.printf("100-player dedupe query measurement: baseline=%d, batched=%d%n", baselineQueries, batchedQueries);
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

  @Test
  void historySearchMatchesKnownQueueLabelGlobal() {
    PlayerEntity player = player("QueueTest", "ESP", "puuid-queue");
    // Record a match with queueId 450 (ARAM)
    playerMatchService.record(player, rankedSoloSummary("EUW1_QUEUE_ARAM", "Lux", true), false);
    MatchEntity match = matchRepository.findByMatchId("EUW1_QUEUE_ARAM").orElseThrow();
    match.setQueueId(450);
    matchRepository.save(match);
    assertEquals(Set.of(player.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory("aram", "", null, null));
  }

  @Test
  void historySearchMatchesFallbackQueueLabelsGlobal() {
    PlayerEntity playerUnknown = player("UnknownQueue", "ESP", "puuid-unknown");
    // Record with queueId null and empty gameMode
    playerMatchService.record(playerUnknown, rankedSoloSummary("EUW1_UNKNOWN", "Lux", true), false);
    MatchEntity unknownMatch = matchRepository.findByMatchId("EUW1_UNKNOWN").orElseThrow();
    unknownMatch.setQueueId(null);
    unknownMatch.setGameMode("");
    matchRepository.save(unknownMatch);
    assertEquals(Set.of(playerUnknown.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory("unknown queue", "", null, null));

    PlayerEntity playerTitle = player("TitleCaseQueue", "ESP", "puuid-title");
    // Record with queueId 999 (unknown) and gameMode CLASSIC to trigger title case fallback
    playerMatchService.record(playerTitle, rankedSoloSummary("EUW1_TITLE", "Lux", true), false);
    MatchEntity titleMatch = matchRepository.findByMatchId("EUW1_TITLE").orElseThrow();
    titleMatch.setQueueId(999);
    titleMatch.setGameMode("ARENA_SPECIAL");
    matchRepository.save(titleMatch);
    assertEquals(Set.of(playerTitle.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory("arena special", "", null, null));
  }

  @Test
  void historySearchTreatsLikeWildcardsLiterallyAcrossLegacyAndGlobalRows() {
    PlayerEntity literalMatch = player("Percent%Player", "ESP", "puuid-literal");
    PlayerEntity wildcardNeighbor = player("PercentXPlayer", "ESP", "puuid-neighbor");
    legacyMatch(literalMatch, "EUW1_LITERAL", "Lux", "CLASSIC", 420, Instant.parse("2030-04-03T18:00:00Z"));
    legacyMatch(wildcardNeighbor, "EUW1_NEIGHBOR", "Lux", "CLASSIC", 420, Instant.parse("2030-04-03T18:00:00Z"));

    assertEquals(
        Set.of(literalMatch.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory("percent%player", "", null, null));

    PlayerEntity literalUnderscore = player("Underscore_Player", "ESP", "puuid-underscore-literal");
    PlayerEntity underscoreNeighbor = player("UnderscoreXPlayer", "ESP", "puuid-underscore-neighbor");
    legacyMatch(literalUnderscore, "EUW1_UNDERSCORE", "Lux", "CLASSIC", 420, Instant.parse("2030-04-03T18:00:00Z"));
    legacyMatch(underscoreNeighbor, "EUW1_UNDERSCORE_NEIGHBOR", "Lux", "CLASSIC", 420, Instant.parse("2030-04-03T18:00:00Z"));
    assertEquals(
        Set.of(literalUnderscore.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory("underscore_player", "", null, null));

    PlayerEntity literalBackslash = player("Back\\slash", "ESP", "puuid-backslash-literal");
    PlayerEntity backslashNeighbor = player("BackXslash", "ESP", "puuid-backslash-neighbor");
    legacyMatch(literalBackslash, "EUW1_BACKSLASH", "Lux", "CLASSIC", 420, Instant.parse("2030-04-03T18:00:00Z"));
    legacyMatch(backslashNeighbor, "EUW1_BACKSLASH_NEIGHBOR", "Lux", "CLASSIC", 420, Instant.parse("2030-04-03T18:00:00Z"));
    assertEquals(
        Set.of(literalBackslash.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory("back\\slash", "", null, null));

    PlayerEntity globalPercent = player("Global%Percent", "ESP", "puuid-global-percent");
    PlayerEntity globalPercentNeighbor = player("GlobalXPercent", "ESP", "puuid-global-percent-neighbor");
    playerMatchService.record(globalPercent, rankedSoloSummary("EUW1_GLOBAL_PERCENT", "Lux", true), false);
    playerMatchService.record(globalPercentNeighbor, rankedSoloSummary("EUW1_GLOBAL_PERCENT_NEIGHBOR", "Lux", true), false);
    assertEquals(Set.of(globalPercent.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory("global%percent", "", null, null));

    PlayerEntity globalUnderscore = player("Global_Underscore", "ESP", "puuid-global-underscore");
    PlayerEntity globalUnderscoreNeighbor = player("GlobalXUnderscore", "ESP", "puuid-global-underscore-neighbor");
    playerMatchService.record(globalUnderscore, rankedSoloSummary("EUW1_GU_LITERAL", "Lux", true), false);
    playerMatchService.record(globalUnderscoreNeighbor, rankedSoloSummary("EUW1_GU_NEIGHBOR", "Lux", true), false);
    assertEquals(Set.of(globalUnderscore.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory("global_underscore", "", null, null));

    PlayerEntity globalBackslash = player("Global\\Slash", "ESP", "puuid-global-backslash");
    PlayerEntity globalBackslashNeighbor = player("GlobalXSlash", "ESP", "puuid-global-backslash-neighbor");
    playerMatchService.record(globalBackslash, rankedSoloSummary("EUW1_GLOBAL_BACKSLASH", "Lux", true), false);
    playerMatchService.record(globalBackslashNeighbor, rankedSoloSummary("EUW1_GLOBAL_BACKSLASH_NEIGHBOR", "Lux", true), false);
    assertEquals(Set.of(globalBackslash.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory("global\\slash", "", null, null));

    PlayerEntity globalChampionUnderscore = player("GlobalChampionUnderscore", "ESP", "puuid-global-champion-underscore");
    PlayerEntity globalChampionUnderscoreNeighbor = player("GlobalChampionUnderscoreNeighbor", "ESP", "puuid-global-champion-underscore-neighbor");
    playerMatchService.record(globalChampionUnderscore, rankedSoloSummary("EUW1_GCU_LITERAL", "Ah_ri", true), false);
    playerMatchService.record(globalChampionUnderscoreNeighbor, rankedSoloSummary("EUW1_GCU_NEIGHBOR", "AhXri", true), false);
    assertEquals(Set.of(globalChampionUnderscore.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory("", "ah_ri", null, null));

    PlayerEntity globalChampionSlash = player("GlobalChampionSlash", "ESP", "puuid-global-champion-slash");
    PlayerEntity globalChampionSlashNeighbor = player("GlobalChampionSlashNeighbor", "ESP", "puuid-global-champion-slash-neighbor");
    playerMatchService.record(globalChampionSlash, rankedSoloSummary("EUW1_GCS_LITERAL", "Ah\\\\ri", true), false);
    playerMatchService.record(globalChampionSlashNeighbor, rankedSoloSummary("EUW1_GCS_NEIGHBOR", "AhXri", true), false);
    assertEquals(Set.of(globalChampionSlash.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory("", "ah\\\\ri", null, null));    PlayerEntity globalChampionLiteral = player("ChampionLiteral", "ESP", "puuid-global-champion-literal");
    PlayerEntity globalChampionNeighbor = player("ChampionNeighbor", "ESP", "puuid-global-champion-neighbor");
    playerMatchService.record(globalChampionLiteral, rankedSoloSummary("EUW1_GLOBAL_CHAMPION_LITERAL", "Ah%ri", true), false);
    playerMatchService.record(globalChampionNeighbor, rankedSoloSummary("EUW1_GLOBAL_CHAMPION_NEIGHBOR", "AhXri", true), false);
    assertEquals(Set.of(globalChampionLiteral.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory("", "ah%ri", null, null));
  }

  @Test
  void historySearchIncludesLegacyOnlyMatchesAndQueueFallbackLabels() {
    PlayerEntity fallbackQueue = player("Fallback", "ESP", "puuid-fallback");
    PlayerEntity other = player("Other", "ESP", "puuid-other");
    legacyMatch(fallbackQueue, "EUW1_FALLBACK", "Lux", "", 987, Instant.parse("2030-04-03T18:00:00Z"));
    legacyMatch(other, "EUW1_OTHER", "Jinx", "CLASSIC", 420, Instant.parse("2030-04-03T18:00:00Z"));
    // Preserve legacy queue-label matching required by issue4.
    assertEquals(Set.of(fallbackQueue.getId()), trackedMatchService.findPlayerIdsMatchingHistory("queue 987", "", null, null));
  }

  @Test
  void historySearchUsesUtcStartInclusiveAndFollowingDayExclusive() {
    PlayerEntity start = player("Start", "ESP", "puuid-start");
    PlayerEntity end = player("End", "ESP", "puuid-end");
    playerMatchService.record(
        start, summaryAt("EUW1_START", "Lux", Instant.parse("2030-04-03T00:00:00Z")), false);
    playerMatchService.record(
        end, summaryAt("EUW1_END", "Jinx", Instant.parse("2030-04-04T00:00:00Z")), false);

    assertEquals(
        Set.of(start.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory(
            "", "", LocalDate.parse("2030-04-03"), LocalDate.parse("2030-04-03")));
    assertEquals(
        Set.of(start.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory(
            "victory", "lux", LocalDate.parse("2030-04-03"), LocalDate.parse("2030-04-03")));
  }

  @Test
  void historySearchWithGlobalNonMatchingAndLegacyMatchingReturnsEmpty() {
    // Global record that does NOT match the query "jinx"
    PlayerEntity globalPlayer = player("GlobalUser", "EUW", "puuid-global");
    playerMatchService.record(
        globalPlayer,
        rankedSoloSummary("EUW1_GLOBAL", "Lux", true),
        false);
    // Legacy record that WOULD match the query "jinx"
    PlayerEntity legacyPlayer = player("LegacyUser", "EUW", "puuid-legacy");
    legacyMatch(legacyPlayer, "EUW1_LEGACY", "Jinx", "CLASSIC", 420, Instant.parse("2030-04-03T18:00:00Z"));
    // Since global has no matching rows, legacy should not be returned
    assertEquals(Set.of(), trackedMatchService.findPlayerIdsMatchingHistory("jinx", "", null, null));
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

  private MatchSummary summaryAt(String matchId, String championName, Instant gameEndAt) {
    return new MatchSummary(
        matchId, 99, championName, true, "CLASSIC", 420, "MIDDLE", "SOLO", 8, 2, 11,
        210, 13200, 26000, 28, 1800, gameEndAt, "EUW1", "EUROPE");
  }

  private TrackedMatchEntity legacyMatch(
      PlayerEntity player,
      String matchId,
      String champion,
      String gameMode,
      Integer queueId,
      Instant gameEndAt) {
    TrackedMatchEntity match = new TrackedMatchEntity();
    match.setPlayer(player);
    match.setMatchId(matchId);
    match.setChampionName(champion);
    match.setResult("VICTORY");
    match.setGameMode(gameMode);
    match.setQueueId(queueId);
    match.setDurationSeconds(1800);
    match.setGameEndAt(gameEndAt);
    match.setNotificationSent(false);
    return trackedMatchRepository.save(match);
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

  private TrackedMatchEntity legacyMatch(
      PlayerEntity player,
      String matchId,
      String championName,
      String gameMode,
      Integer queueId,
      Instant gameEndAt) {
    TrackedMatchEntity match = new TrackedMatchEntity();
    match.setPlayer(player);
    match.setMatchId(matchId);
    match.setChampionName(championName);
    match.setResult("VICTORY");
    match.setGameMode(gameMode);
    match.setQueueId(queueId);
    match.setDurationSeconds(1800);
    match.setGameEndAt(gameEndAt);
    match.setNotificationSent(false);
    return trackedMatchRepository.save(match);
  }
}
