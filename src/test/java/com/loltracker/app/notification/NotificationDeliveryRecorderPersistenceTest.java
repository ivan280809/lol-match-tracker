package com.loltracker.app.notification;

import static org.junit.jupiter.api.Assertions.*;

import com.loltracker.app.match.TrackedMatchEntity;
import com.loltracker.app.match.TrackedMatchRepository;
import com.loltracker.app.player.PlayerEntity;
import com.loltracker.app.player.PlayerRepository;
import com.loltracker.lolmatchtracker.LolMatchTrackerApplication;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.HttpHeaders;

/**
 * Integration test for the {@link NotificationDeliveryRecorder} ensuring that a 429
 * response results in a properly truncated error message and that the global
 * cooldown is applied to all eligible outbox rows.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = LolMatchTrackerApplication.class,
    properties = {
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false"
    })
@Transactional
class NotificationDeliveryRecorderPersistenceTest {

  @Autowired private NotificationDeliveryRecorder recorder;
  @Autowired private NotificationOutboxRepository outboxRepo;
  @Autowired private PlayerRepository playerRepo;
  @Autowired private TrackedMatchRepository matchRepo;
  @Autowired private EntityManager entityManager;

  @Test
  void recordFailureTruncatesLong429MessageAndAppliesGlobalCooldown() {
    // Arrange – create a player and tracked match
    PlayerEntity player = new PlayerEntity();
    player.setGameName("player");
    player.setTagLine("EUW");
    playerRepo.save(player);

    TrackedMatchEntity match = new TrackedMatchEntity();
    match.setPlayer(player);
    match.setMatchId("match123");
    match.setChampionName("ahri");
    match.setChampionId(1);
    match.setResult("WIN");
    match.setGameMode("URF");
    match.setQueueId(420);
    match.setLane("MID");
    match.setRole("MIDDLE");
    match.setKills(10);
    match.setDeaths(2);
    match.setAssists(8);
    match.setCreepScore(200);
    match.setGoldEarned(15000);
    match.setDamageDealtToChampions(20000);
    match.setVisionScore(40);
    match.setDurationSeconds(1800L);
    match.setGameEndAt(Instant.now());
    match.setPlatform("EUW1");
    match.setRegion("EUROPE");
    match.setNotificationSent(false);
    matchRepo.save(match);

    NotificationOutboxEntity outbox = new NotificationOutboxEntity();
    outbox.setTrackedMatch(match);
    outbox.setStatus(NotificationDeliveryStatus.PENDING);
    outbox.setAttemptCount(1);
    outbox.setNextAttemptAt(Instant.now());
    outboxRepo.save(outbox);

    // Add a second outbox that should be affected by the global cooldown.  It must
    // reference a distinct tracked match, otherwise the unique constraint on
    // `tracked_match_id` would be violated.
    TrackedMatchEntity match2 = new TrackedMatchEntity();
    match2.setPlayer(player);
    match2.setMatchId("match456");
    match2.setChampionName("ahri");
    match2.setChampionId(1);
    match2.setResult("WIN");
    match2.setGameMode("URF");
    match2.setQueueId(420);
    match2.setLane("MID");
    match2.setRole("MIDDLE");
    match2.setKills(10);
    match2.setDeaths(2);
    match2.setAssists(8);
    match2.setCreepScore(200);
    match2.setGoldEarned(15000);
    match2.setDamageDealtToChampions(20000);
    match2.setVisionScore(40);
    match2.setDurationSeconds(1800L);
    match2.setGameEndAt(Instant.now());
    match2.setPlatform("EUW1");
    match2.setRegion("EUROPE");
    match2.setNotificationSent(false);
    matchRepo.save(match2);

    NotificationOutboxEntity second = new NotificationOutboxEntity();
    second.setTrackedMatch(match2);
    second.setStatus(NotificationDeliveryStatus.PENDING);
    second.setAttemptCount(1);
    second.setNextAttemptAt(Instant.now());
    outboxRepo.save(second);

    // Create a very long 429 message
    String longMsg = "x".repeat(600); // 600 chars
    RestClientResponseException exception = new RestClientResponseException(
        longMsg,
        429,
        "Too Many Requests",
        new HttpHeaders(),
        longMsg.getBytes(StandardCharsets.UTF_8),
        StandardCharsets.UTF_8);

    // Act
    recorder.recordFailure(outbox, exception);
    entityManager.flush();
    entityManager.clear();

    // Assert against rows reloaded after the write is flushed to H2.
    Optional<NotificationOutboxEntity> opt = outboxRepo.findById(outbox.getId());
    assertTrue(opt.isPresent(), "Outbox should exist");
    NotificationOutboxEntity persisted = opt.get();
    String lastError = persisted.getLastError();
    assertNotNull(lastError, "Last error should be set");
    assertTrue(
        lastError.length() <= 500,
        "Last error should be truncated to 500 chars – was " + lastError.length());
    assertTrue(
        lastError.startsWith(NotificationDeliveryRecorder.TELEGRAM_RATE_LIMIT_ERROR_PREFIX),
        "Prefix should be retained");
    // Ensure cooldown applied to second outbox
    Optional<NotificationOutboxEntity> opt2 = outboxRepo.findById(second.getId());
    assertTrue(opt2.isPresent());
    assertEquals(
        persisted.getNextAttemptAt(),
        opt2.get().getNextAttemptAt(),
        "Global cooldown should be applied to all eligible rows");
  }
}
