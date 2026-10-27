package com.loltracker.app.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loltracker.app.ops.PollLease;
import com.loltracker.app.ops.PollLockRepository;
import com.loltracker.app.ops.PollLockService;
import com.loltracker.app.player.PlayerEntity;
import com.loltracker.app.player.PlayerRepository;
import com.loltracker.lolmatchtracker.LolMatchTrackerApplication;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.List;
import java.util.function.Supplier;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(
    classes = LolMatchTrackerApplication.class,
    properties = {
      "spring.datasource.driver-class-name=org.postgresql.Driver",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
      "spring.flyway.enabled=true"
    })
@DirtiesContext
class PostgreSqlPersistenceTest {

  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
      "postgres@sha256:c961aa287d8698297cb26cdfadfbe9fd2cbaf77e53cfffe9636e8d8a1e4d842c");

  @DynamicPropertySource
  static void configurePostgreSql(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private PlayerRepository playerRepository;
  @Autowired private MatchRepository matchRepository;
  @Autowired private PlayerMatchRepository playerMatchRepository;
  @Autowired private PlayerMatchService playerMatchService;
  @Autowired private TrackedMatchService trackedMatchService;
  @Autowired private TrackedMatchRepository trackedMatchRepository;
  @Autowired private PollLockRepository pollLockRepository;
  @Autowired private PollLockService pollLockService;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    playerMatchRepository.deleteAll();
    matchRepository.deleteAll();
    trackedMatchRepository.deleteAll();
    playerRepository.deleteAll();
    pollLockRepository.deleteAll();
  }

  @Test
  void pollLockLeaseRoundTripsOnPostgreSql() {
    Optional<PollLease> firstLease = pollLockService.acquire(Duration.ofMinutes(5));

    assertTrue(firstLease.isPresent());
    assertTrue(pollLockService.acquire(Duration.ofMinutes(5)).isEmpty());

    pollLockService.release(firstLease.orElseThrow());

    Optional<PollLease> secondLease = pollLockService.acquire(Duration.ofMinutes(5));
    assertTrue(secondLease.isPresent());
  }

  @Test
  void playerMatchDedupeKeepsSingleGlobalMatchOnPostgreSql() {
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
  }

  @Test
  void historySearchPreservesLiteralLikeMatchingOnPostgreSql() {
    PlayerEntity literalPlayer = player("Percent%Player", "ESP", "puuid-literal");
    PlayerEntity similarPlayer = player("PercentXPlayer", "ESP", "puuid-similar");
    playerMatchService.record(
        literalPlayer,
        new MatchSummary(
            "EUW1_LITERAL", 99, "Lux", true, "CLASSIC", 420, "MIDDLE", "SOLO", 8, 2, 11,
            210, 13200, 26000, 28, 1800, Instant.parse("2030-04-03T18:00:00Z"), "EUW1", "EUROPE"),
        false);
    playerMatchService.record(
        similarPlayer,
        new MatchSummary(
            "EUW1_SIMILAR", 99, "Lux", true, "CLASSIC", 420, "MIDDLE", "SOLO", 8, 2, 11,
            210, 13200, 26000, 28, 1800, Instant.parse("2030-04-03T18:00:00Z"), "EUW1", "EUROPE"),
        false);

    assertEquals(
        Set.of(literalPlayer.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory("percent%player", "", null, null));
  }

  @Test
  void historyQueueFallbackMatchesJavaWhitespaceSemanticsOnPostgreSql() {
    PlayerEntity nullQueue = player("NullQueueWhitespace", "EUW", "puuid-null-queue-ws");
    PlayerEntity blankUnknownQueue = player("BlankUnknownQueue", "EUW", "puuid-unknown-queue-ws");
    PlayerEntity nonblankUnknownQueue = player("NonblankUnknownQueue", "EUW", "puuid-unknown-queue-mode");
    PlayerEntity wrappedUnknownMode = player("WrappedUnknownMode", "EUW", "puuid-wrapped-unknown-mode");
    playerMatchService.record(
        nullQueue,
        new MatchSummary(
            "EUW1_NULL_QUEUE_WS", 99, "Lux", true, "\u001c\u2003", null, "MIDDLE", "SOLO", 8, 2, 11,
            210, 13200, 26000, 28, 1800, Instant.parse("2030-04-03T18:00:00Z"), "EUW1", "EUROPE"),
        false);
    playerMatchService.record(
        blankUnknownQueue,
        new MatchSummary(
            "EUW1_UNKNOWN_QUEUE_WS", 99, "Lux", true, "\u2003\u001c", 999, "MIDDLE", "SOLO", 8, 2, 11,
            210, 13200, 26000, 28, 1800, Instant.parse("2030-04-03T18:00:00Z"), "EUW1", "EUROPE"),
        false);
    playerMatchService.record(
        nonblankUnknownQueue,
        new MatchSummary(
            "EUW1_UNKNOWN_QUEUE_MODE", 99, "Lux", true, "\u2003CLASSIC\u2003", 998, "MIDDLE", "SOLO", 8, 2, 11,
            210, 13200, 26000, 28, 1800, Instant.parse("2030-04-03T18:00:00Z"), "EUW1", "EUROPE"),
        false);
    playerMatchService.record(
        wrappedUnknownMode,
        new MatchSummary(
            "EUW1_UNKNOWN_QUEUE_WRAPPED", 99, "Lux", true, "\tARENA_SPECIAL\n", 997, "MIDDLE", "SOLO", 8, 2, 11,
            210, 13200, 26000, 28, 1800, Instant.parse("2030-04-03T18:00:00Z"), "EUW1", "EUROPE"),
        false);

    // Exercise raw persisted whitespace. The recording service deliberately
    // normalizes blank incoming game modes to "Unknown" before persistence.
    jdbcTemplate.update("UPDATE matches SET game_mode = ? WHERE match_id = ?",
        "\u001c\u2003", "EUW1_NULL_QUEUE_WS");
    jdbcTemplate.update("UPDATE matches SET game_mode = ? WHERE match_id = ?",
        "\u2003\u001c", "EUW1_UNKNOWN_QUEUE_WS");

    assertEquals(
        Set.of(nullQueue.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory("unknown queue", "", null, null));
    assertEquals(
        Set.of(blankUnknownQueue.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory("queue 999", "", null, null));
    assertEquals(
        Set.of(wrappedUnknownMode.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory("arena special", "", null, null));
    assertEquals(
        Set.of(),
        trackedMatchService.findPlayerIdsMatchingHistory("queue 998", "", null, null));
  }

  @Test
  void historySearchPreservesJavaRootCaseConversionForGreekFinalSigma() {
    PlayerEntity greekPlayer = player("\u039f\u03a3", "EUW", "puuid-greek-case");
    playerMatchService.record(
        greekPlayer,
        new MatchSummary(
            "EUW1_GREEK_CASE", 99, "Lux", true, "CLASSIC", 420, "MIDDLE", "SOLO", 8, 2, 11,
            210, 13200, 26000, 28, 1800, Instant.parse("2030-04-03T18:00:00Z"), "EUW1", "EUROPE"),
        false);

    Set<Long> expected = Set.of(greekPlayer.getId());
    assertEquals(expected, trackedMatchService.findPlayerIdsMatchingHistory("\u03bf\u03c2", "", null, null));
  }

  @Test
  void historySearchTreatsSqlWildcardsLiterallyOnPostgreSql() {
    PlayerEntity percentLiteral = player("Percent%Player", "ESP", "pg-percent-literal");
    PlayerEntity percentNeighbor = player("PercentXPlayer", "ESP", "pg-percent-neighbor");
    recordSearchMatch(percentLiteral, "EUW1_PG_PERCENT_LITERAL", "Lux");
    recordSearchMatch(percentNeighbor, "EUW1_PG_PERCENT_NEIGHBOR", "Lux");
    assertEquals(Set.of(percentLiteral.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory("percent%player", "", null, null));

    PlayerEntity underscoreLiteral = player("Under_score", "ESP", "pg-underscore-literal");
    PlayerEntity underscoreNeighbor = player("UnderXscore", "ESP", "pg-underscore-neighbor");
    recordSearchMatch(underscoreLiteral, "EUW1_PG_UNDERSCORE_LITERAL", "Lux");
    recordSearchMatch(underscoreNeighbor, "EUW1_PG_UNDERSCORE_NEIGHBOR", "Lux");
    assertEquals(Set.of(underscoreLiteral.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory("under_score", "", null, null));

    PlayerEntity slashLiteral = player("Back\\slash", "ESP", "pg-slash-literal");
    PlayerEntity slashNeighbor = player("BackXslash", "ESP", "pg-slash-neighbor");
    recordSearchMatch(slashLiteral, "EUW1_PG_SLASH_LITERAL", "Lux");
    recordSearchMatch(slashNeighbor, "EUW1_PG_SLASH_NEIGHBOR", "Lux");
    assertEquals(Set.of(slashLiteral.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory("back\\slash", "", null, null));

    PlayerEntity championLiteral = player("ChampionLiteral", "ESP", "pg-champion-literal");
    PlayerEntity championNeighbor = player("ChampionNeighbor", "ESP", "pg-champion-neighbor");
    recordSearchMatch(championLiteral, "EUW1_PG_CHAMPION_LITERAL", "Ah%ri");
    recordSearchMatch(championNeighbor, "EUW1_PG_CHAMPION_NEIGHBOR", "AhXri");
    assertEquals(Set.of(championLiteral.getId()),
        trackedMatchService.findPlayerIdsMatchingHistory("", "ah%ri", null, null));
  }

  @Test
  void benchmarkHistorySearchAtOneHundredThousandPlayerMatchRelations() throws Exception {
    List<PlayerEntity> players = new ArrayList<>(1_000);
    for (int i = 0; i < 1_000; i++) {
      PlayerEntity player = new PlayerEntity();
      player.setGameName("Player%04d".formatted(i));
      player.setTagLine("EUW");
      player.setPuuid("benchmark-puuid-%04d".formatted(i));
      player.setActive(true);
      players.add(player);
    }
    players = playerRepository.saveAllAndFlush(players);

    List<MatchEntity> matches = new ArrayList<>(100);
    for (int i = 0; i < 100; i++) {
      MatchEntity match = new MatchEntity();
      match.setMatchId("EUW1_BENCH_%03d".formatted(i));
      match.setGameMode("CLASSIC");
      match.setQueueId(420);
      match.setDurationSeconds(1_800);
      match.setGameEndAt(Instant.parse("2030-04-03T18:00:00Z")
          .plus(Duration.ofDays(i % 2))
          .plusSeconds(i));
      match.setPlatform("EUW1");
      match.setRegion("EUROPE");
      matches.add(match);
    }
    matches = matchRepository.saveAllAndFlush(matches);

    String insert = """
        INSERT INTO player_matches
          (player_id, match_id, puuid, champion_id, champion_name, result, lane, role,
           kills, deaths, assists, creep_score, gold_earned, damage_dealt_to_champions,
           vision_score, notification_suppressed)
        VALUES (?, ?, ?, ?, ?, 'VICTORY', 'MIDDLE', 'SOLO', 8, 2, 11, 210, 13200, 26000, 28, FALSE)
        """;
    List<Object[]> batch = new ArrayList<>(1_000);
    int inserted = 0;
    for (PlayerEntity player : players) {
      for (int matchIndex = 0; matchIndex < matches.size(); matchIndex++) {
        batch.add(new Object[] {
            player.getId(), matches.get(matchIndex).getId(), player.getPuuid(), 99,
            matchIndex % 2 == 0 ? "Lux" : "Jinx"
        });
        if (batch.size() == 1_000) {
          jdbcTemplate.batchUpdate(insert, batch);
          inserted += batch.size();
          batch.clear();
        }
      }
    }
    if (!batch.isEmpty()) {
      jdbcTemplate.batchUpdate(insert, batch);
      inserted += batch.size();
    }
    assertEquals(100_000, inserted);
    assertEquals(100_000, playerMatchRepository.count());

    String text = "player0001";
    String champion = "lux";
    LocalDate fromDate = LocalDate.parse("2030-04-03");
    LocalDate toDate = LocalDate.parse("2030-04-03");
    Set<Long> expected = Set.of(players.get(1).getId());
    BaselineSearch baselineSearch = baselineHistorySearch(text, champion, fromDate, toDate);
    assertEquals(100_000, baselineSearch.materializedEntities());
    Set<Long> baselineResult = baselineSearch.playerIds();
    Set<Long> optimizedResult =
        trackedMatchService.findPlayerIdsMatchingHistory(text, champion, fromDate, toDate);
    assertEquals(expected, baselineResult);
    assertEquals(baselineResult, optimizedResult);

    Measurement baseline = measure(
        "baseline-java-filter", () -> baselineHistorySearch(text, champion, fromDate, toDate));
    Measurement optimized = measure(
        "postgresql-filtered-ids",
        () -> new BaselineSearch(
            trackedMatchService.findPlayerIdsMatchingHistory(text, champion, fromDate, toDate), 0));
    assertEquals(100_000, baseline.materializedEntities());
    assertEquals(0, optimized.materializedEntities());
    assertEquals(1, optimized.resultIds());
    String historySql = PlayerMatchRepository.class.getMethod(
        "findDistinctPlayerIdsMatchingHistory",
        String.class, String.class, Instant.class, Instant.class, String.class)
        .getAnnotation(Query.class).value();
    String optimizedExplain = new NamedParameterJdbcTemplate(jdbcTemplate).queryForObject(
        "EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) " + historySql,
        new MapSqlParameterSource()
            .addValue("champion", champion)
            .addValue("text", text)
            .addValue("from", java.sql.Timestamp.from(fromDate.atStartOfDay().toInstant(ZoneOffset.UTC)))
            .addValue("to", java.sql.Timestamp.from(toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)))
            .addValue("javaWhitespace", MatchQueueCatalog.JAVA_WHITESPACE),
        String.class);
    String baselineExplain = jdbcTemplate.queryForObject(
        """
        EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)
        SELECT pm.*, p.*, m.*
        FROM player_matches pm
        JOIN players p ON p.id = pm.player_id
        JOIN matches m ON m.id = pm.match_id
        """,
        String.class);
    System.out.printf(
        java.util.Locale.ROOT,
        "HISTORY_BENCHMARK relations=%d baseline_entities_materialized=%d baseline_result_ids=%d optimized_result_ids=%d samples=%d baseline_p95_ms=%.3f baseline_sampled_peak_heap_delta_bytes=%d optimized_p95_ms=%.3f optimized_sampled_peak_heap_delta_bytes=%d%n",
        inserted, baseline.materializedEntities(), baseline.resultIds(), optimized.resultIds(), 30, baseline.p95Millis(),
        baseline.peakHeapDeltaBytes(), optimized.p95Millis(), optimized.peakHeapDeltaBytes());
    System.out.println("HISTORY_BENCHMARK_EXPLAIN_BASELINE_JSON=" + baselineExplain);
    System.out.println("HISTORY_BENCHMARK_EXPLAIN_OPTIMIZED_JSON=" + optimizedExplain);
    System.out.printf("HISTORY_BENCHMARK_RUNTIME java=%s postgres=%s cpus=%d os=%s%n",
        System.getProperty("java.version"), jdbcTemplate.queryForObject("SELECT version()", String.class),
        Runtime.getRuntime().availableProcessors(), System.getProperty("os.name"));
    assertTrue(optimized.p95Millis() < 500.0,
        "Optimized history search p95=" + optimized.p95Millis() + " ms must remain below 500 ms");
  }

  /** Recreates the former full materialization followed by Java-side filtering. */
  private BaselineSearch baselineHistorySearch(
      String text, String champion, LocalDate fromDate, LocalDate toDate) {
    List<PlayerMatchEntity> materialized = playerMatchRepository.findAllForHistorySearch();
    String normalizedText = text.toLowerCase(java.util.Locale.ROOT);
    String normalizedChampion = champion.toLowerCase(java.util.Locale.ROOT);
    Instant from = fromDate.atStartOfDay().toInstant(ZoneOffset.UTC);
    Instant to = toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    Set<Long> ids = materialized.stream()
        .filter(relation -> !relation.getMatch().getGameEndAt().isBefore(from))
        .filter(relation -> relation.getMatch().getGameEndAt().isBefore(to))
        .filter(relation -> contains(relation.getChampionName(), normalizedChampion))
        .filter(relation -> matchesText(relation, normalizedText))
        .map(relation -> relation.getPlayer().getId())
        .collect(java.util.stream.Collectors.toSet());
    return new BaselineSearch(ids, materialized.size());
  }

  private boolean matchesText(PlayerMatchEntity relation, String query) {
    MatchEntity match = relation.getMatch();
    PlayerEntity player = relation.getPlayer();
    return contains(relation.getChampionName(), query)
        || contains(match.getGameMode(), query)
        || contains(match.getMatchId(), query)
        || contains(relation.getResult().name(), query)
        || contains(player.getGameName(), query)
        || contains(player.getTagLine(), query)
        || contains(MatchQueueCatalog.describe(match.getQueueId(), match.getGameMode()).label(), query);
  }

  private boolean contains(String value, String query) {
    return value != null && value.toLowerCase(java.util.Locale.ROOT).contains(query);
  }

  private Measurement measure(String name, Supplier<BaselineSearch> operation) throws InterruptedException {
    for (int i = 0; i < 2; i++) {
      operation.get();
    }
    List<Long> samples = new ArrayList<>(30);
    long maxPeakDelta = 0;
    long materializedEntities = 0;
    long resultIds = 0;
    for (int i = 0; i < 30; i++) {
      System.gc();
      Thread.sleep(20);
      long usedBefore = heapUsed();
      AtomicBoolean sampling = new AtomicBoolean(true);
      AtomicLong sampledPeak = new AtomicLong(usedBefore);
      Thread sampler = new Thread(() -> {
        while (sampling.get()) {
          sampledPeak.accumulateAndGet(heapUsed(), Math::max);
          try {
            Thread.sleep(1);
          } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return;
          }
        }
      }, "history-heap-sampler");
      sampler.setDaemon(true);
      sampler.start();
      long started = System.nanoTime();
      long elapsed;
      BaselineSearch result;
      try {
        result = operation.get();
        elapsed = System.nanoTime() - started;
      } finally {
        sampling.set(false);
        sampler.join();
      }
      sampledPeak.accumulateAndGet(heapUsed(), Math::max);
      samples.add(elapsed);
      materializedEntities = result.materializedEntities();
      resultIds = result.playerIds().size();
      maxPeakDelta = Math.max(maxPeakDelta, Math.max(0, sampledPeak.get() - usedBefore));
    }
    Collections.sort(samples);
    double p95 = samples.get((int) Math.ceil(samples.size() * 0.95) - 1) / 1_000_000.0;
    return new Measurement(p95, maxPeakDelta, materializedEntities, resultIds);
  }

  private long heapUsed() {
    return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
  }

  private record BaselineSearch(Set<Long> playerIds, int materializedEntities) {}

  private record Measurement(
      double p95Millis, long peakHeapDeltaBytes, long materializedEntities, long resultIds) {}

  private void recordSearchMatch(PlayerEntity player, String matchId, String championName) {
    playerMatchService.record(
        player,
        new MatchSummary(
            matchId, 99, championName, true, "CLASSIC", 420, "MIDDLE", "SOLO", 8, 2, 11,
            210, 13200, 26000, 28, 1800, Instant.parse("2030-04-03T18:00:00Z"), "EUW1", "EUROPE"),
        false);
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
