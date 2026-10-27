package com.loltracker.app.ops;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.loltracker.app.player.PlayerEntity;
import com.loltracker.lolmatchtracker.LolMatchTrackerApplication;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(
    classes = LolMatchTrackerApplication.class,
    properties = {
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false"
    })
@DirtiesContext
class PollRunServiceProgressSchedulerIntegrationTest {
  @Autowired private PollRunService pollRunService;
  @Autowired private PollRunRepository pollRunRepository;
  @Autowired private TaskScheduler taskScheduler;

  @Test
  void realSchedulerPersistsLatestProgressWithoutAnotherUpdateCall() throws Exception {
    assertNotNull(taskScheduler, "The application scheduler must be active in this integration test");
    java.util.concurrent.CountDownLatch busy = new java.util.concurrent.CountDownLatch(1);
    java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
    taskScheduler.schedule(() -> {
      busy.countDown();
      try { release.await(15, java.util.concurrent.TimeUnit.SECONDS); }
      catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
    }, Instant.now());
    assertTrue(busy.await(2, java.util.concurrent.TimeUnit.SECONDS));
    try {
    PollRunEntity run = pollRunService.startRun();
    PlayerEntity first = player("first");
    PlayerEntity latest = player("latest");

    pollRunService.updateProgress(run, first, "Procesando jugadores");
    Instant changedAt = Instant.now();
    pollRunService.updateProgress(run, latest, "Procesando jugadores");

    Instant deadline = changedAt.plusSeconds(5);
    PollRunEntity visible = null;
    Instant observedAt = null;
    while (Instant.now().isBefore(deadline.plusMillis(20))) {
      visible = pollRunRepository.findById(run.getId()).orElseThrow();
      if ("latest#EUW".equals(visible.getCurrentPlayer())) {
        observedAt = Instant.now();
        break;
      }
      Thread.sleep(2);
    }

    assertNotNull(visible);
    assertNotNull(observedAt, "The real scheduler must persist pending progress by the five-second deadline");
    assertEquals("latest#EUW", visible.getCurrentPlayer());
    long observedMillis = Duration.between(changedAt, observedAt).toMillis();
    System.out.printf("Real scheduler progress visibility: %d ms%n", observedMillis);
    assertTrue(observedMillis <= 5_000, "Progress must be visible within five seconds");
    } finally {
      release.countDown();
    }
  }

  private PlayerEntity player(String name) {
    PlayerEntity player = new PlayerEntity();
    player.setGameName(name);
    player.setTagLine("EUW");
    return player;
  }

  @Test
  void finalStageVisibleWithinFiveSecondsAfterHundredUpdates() throws Exception {
    PollRunEntity run = pollRunService.startRun();
    for (int index = 0; index < 100; index++) {
      pollRunService.updateProgress(run, player("player" + index), "Processing many players");
    }
    Instant updatedAt = Instant.now();
    Instant deadline = updatedAt.plusSeconds(5);
    PollRunEntity visible;
    do {
      visible = pollRunRepository.findById(run.getId()).orElseThrow();
      if ("player99#EUW".equals(visible.getCurrentPlayer())) break;
      Thread.sleep(10);
    } while (Instant.now().isBefore(deadline));
    assertEquals("player99#EUW", visible.getCurrentPlayer());
    assertEquals("Processing many players", visible.getCurrentStage());
    assertTrue(Duration.between(updatedAt, Instant.now()).toMillis() <= 5_000);
  }
}
