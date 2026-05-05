package com.loltracker.app.match;

import static org.junit.jupiter.api.Assertions.*;

import com.loltracker.app.player.PlayerEntity;
import com.loltracker.app.player.PlayerRepository;
import com.loltracker.lolmatchtracker.LolMatchTrackerApplication;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(classes = LolMatchTrackerApplication.class)
@DirtiesContext
class PersistenceIntegrationTest {

  @Autowired private PlayerRepository playerRepository;
  @Autowired private TrackedMatchRepository trackedMatchRepository;
  @Autowired private MatchRepository matchRepository;
  @Autowired private PlayerMatchRepository playerMatchRepository;
  @Autowired private PlayerMatchService playerMatchService;

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

  private PlayerEntity player(String gameName, String tagLine, String puuid) {
    PlayerEntity player = new PlayerEntity();
    player.setGameName(gameName);
    player.setTagLine(tagLine);
    player.setPuuid(puuid);
    player.setActive(true);
    return playerRepository.save(player);
  }
}
