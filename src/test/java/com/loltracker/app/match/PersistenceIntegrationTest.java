package com.loltracker.app.match;

import static org.junit.jupiter.api.Assertions.*;

import com.loltracker.app.player.PlayerEntity;
import com.loltracker.app.player.PlayerRepository;
import com.loltracker.lolmatchtracker.LolMatchTrackerApplication;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(classes = LolMatchTrackerApplication.class)
@DirtiesContext
class PersistenceIntegrationTest {

  @Autowired private PlayerRepository playerRepository;
  @Autowired private TrackedMatchRepository trackedMatchRepository;

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
}
