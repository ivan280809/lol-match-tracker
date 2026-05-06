package com.loltracker.app.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class FlywayMigrationTest {

  @Test
  void migrationsBackfillLegacyTrackedMatchesIntoGlobalTables() {
    DriverManagerDataSource dataSource = dataSource();
    migrate(dataSource, "1");
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);

    long firstPlayerId = insertPlayer(jdbc, "Bazaga", "ESP", "puuid-1");
    long secondPlayerId = insertPlayer(jdbc, "LuxMain", "EUW", "puuid-2");
    insertTrackedMatch(jdbc, firstPlayerId, "EUW1_SHARED", "Lux", "VICTORY", false);
    insertTrackedMatch(jdbc, secondPlayerId, "EUW1_SHARED", "LeeSin", "DEFEAT", true);

    migrate(dataSource, null);
    migrate(dataSource, null);

    assertEquals(1L, count(jdbc, "matches"));
    assertEquals(2L, count(jdbc, "player_matches"));
    assertEquals(1L, countWhere(jdbc, "matches", "match_id = ?", "EUW1_SHARED"));
    assertEquals(
        "puuid-1",
        jdbc.queryForObject(
            """
            SELECT pm.puuid
            FROM player_matches pm
            JOIN matches m ON m.id = pm.match_id
            WHERE pm.player_id = ? AND m.match_id = ?
            """,
            String.class,
            firstPlayerId,
            "EUW1_SHARED"));
    assertEquals(
        Boolean.TRUE,
        jdbc.queryForObject(
            """
            SELECT pm.notification_suppressed
            FROM player_matches pm
            JOIN matches m ON m.id = pm.match_id
            WHERE pm.player_id = ? AND m.match_id = ?
            """,
            Boolean.class,
            secondPlayerId,
            "EUW1_SHARED"));
  }

  @Test
  void migrationsValidateAndEnforceSafePuuidUniqueness() {
    DriverManagerDataSource dataSource = dataSource();
    Flyway flyway = migrate(dataSource, null);
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);

    insertPlayer(jdbc, "NoPuuidOne", "EUW", null);
    insertPlayer(jdbc, "NoPuuidTwo", "EUW", null);
    insertPlayer(jdbc, "HasPuuid", "EUW", "shared-puuid");

    assertThrows(DuplicateKeyException.class, () -> insertPlayer(jdbc, "DuplicatePuuid", "EUW", "shared-puuid"));
    flyway.validate();
  }

  private Flyway migrate(DriverManagerDataSource dataSource, String target) {
    FluentConfiguration configuration =
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration");
    if (target != null) {
      configuration.target(target);
    }
    Flyway flyway = configuration.load();
    flyway.migrate();
    return flyway;
  }

  private DriverManagerDataSource dataSource() {
    return new DriverManagerDataSource(
        "jdbc:h2:mem:migration-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "sa",
        "");
  }

  private long insertPlayer(JdbcTemplate jdbc, String gameName, String tagLine, String puuid) {
    jdbc.update(
        "INSERT INTO players (game_name, tag_line, puuid) VALUES (?, ?, ?)",
        gameName,
        tagLine,
        puuid);
    return jdbc.queryForObject(
        "SELECT id FROM players WHERE game_name = ? AND tag_line = ?", Long.class, gameName, tagLine);
  }

  private void insertTrackedMatch(
      JdbcTemplate jdbc,
      long playerId,
      String matchId,
      String championName,
      String result,
      boolean notificationSent) {
    jdbc.update(
        """
        INSERT INTO tracked_matches (
            player_id,
            match_id,
            champion_name,
            result,
            game_mode,
            queue_id,
            lane,
            role,
            kills,
            deaths,
            assists,
            creep_score,
            gold_earned,
            damage_dealt_to_champions,
            vision_score,
            duration_seconds,
            game_end_at,
            platform,
            region,
            notification_sent
        )
        VALUES (?, ?, ?, ?, 'CLASSIC', 420, 'MIDDLE', 'SOLO', 8, 2, 11, 210, 13200, 26000, 28, 1800,
            TIMESTAMP WITH TIME ZONE '2030-04-03 18:00:00Z', 'EUW1', 'EUROPE', ?)
        """,
        playerId,
        matchId,
        championName,
        result,
        notificationSent);
  }

  private long count(JdbcTemplate jdbc, String table) {
    return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
  }

  private long countWhere(JdbcTemplate jdbc, String table, String predicate, Object... args) {
    return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + predicate, Long.class, args);
  }
}
