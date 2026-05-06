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
import java.util.Optional;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
    classes = LolMatchTrackerApplication.class,
    properties = {
      "spring.datasource.driver-class-name=org.postgresql.Driver",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
      "spring.flyway.enabled=false"
    })
@DirtiesContext
class PostgreSqlPersistenceTest {

  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void configurePostgreSql(DynamicPropertyRegistry registry) {
    migratePostgreSqlSchema();
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  private static void migratePostgreSqlSchema() {
    if (!postgres.isRunning()) {
      postgres.start();
    }
    Flyway.configure()
        .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
        .locations("classpath:db/migration")
        .load()
        .migrate();
  }

  @Autowired private PlayerRepository playerRepository;
  @Autowired private MatchRepository matchRepository;
  @Autowired private PlayerMatchRepository playerMatchRepository;
  @Autowired private PlayerMatchService playerMatchService;
  @Autowired private PollLockRepository pollLockRepository;
  @Autowired private PollLockService pollLockService;

  @BeforeEach
  void setUp() {
    playerMatchRepository.deleteAll();
    matchRepository.deleteAll();
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

  private PlayerEntity player(String gameName, String tagLine, String puuid) {
    PlayerEntity player = new PlayerEntity();
    player.setGameName(gameName);
    player.setTagLine(tagLine);
    player.setPuuid(puuid);
    player.setActive(true);
    return playerRepository.save(player);
  }
}
