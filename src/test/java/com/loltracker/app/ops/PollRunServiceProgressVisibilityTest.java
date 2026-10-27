package com.loltracker.app.ops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loltracker.app.player.PlayerEntity;
import com.loltracker.lolmatchtracker.LolMatchTrackerApplication;
import com.loltracker.app.ops.PollRunEntity;
import com.loltracker.app.ops.PollRunRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Test that after many rapid progress updates the final stage is persisted within five seconds.
 */
@SpringBootTest(
    classes = LolMatchTrackerApplication.class,
    properties = {
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false"
    })
@DirtiesContext
public class PollRunServiceProgressVisibilityTest {
  @Autowired
  private PollRunService pollRunService;
  @Autowired
  private PollRunRepository pollRunRepository;
  @Autowired
  private TaskScheduler taskScheduler;

  @Test
  public void finalStageVisibleWithinFiveSecondsAfterHundredUpdates() throws InterruptedException {
    assertNotNull(taskScheduler, "TaskScheduler must be present for this test");
    PollRunEntity run = pollRunService.startRun();
    String stage = "Processing many players";
    // Rapid updates
    for (int i = 0; i < 100; i++) {
      PlayerEntity p = new PlayerEntity();
      p.setGameName("player" + i);
      p.setTagLine("EUW");
      pollRunService.updateProgress(run, p, stage);
      // Sleep a tiny bit to avoid overwhelming scheduler
      TimeUnit.MILLISECONDS.sleep(10);
    }
    // Wait for flush to occur (max 5 seconds after last update)
    Instant lastUpdateAt = Instant.now();
    Instant deadline = lastUpdateAt.plusSeconds(5);
    PollRunEntity visible = null;
    Instant observedAt = null;
    while (Instant.now().isBefore(deadline)) {
      visible = pollRunRepository.findById(run.getId()).orElseThrow();
      if (("player99" + "#EUW").equals(visible.getCurrentPlayer())) {
        observedAt = Instant.now();
        break;
      }
      TimeUnit.MILLISECONDS.sleep(50);
    }
    assertNotNull(visible, "Run should be found");
    assertNotNull(observedAt, "Final stage should be visible within five seconds");
    assertEquals("player99#EUW", visible.getCurrentPlayer());
    assertEquals(stage, visible.getCurrentStage());
    long elapsed = Duration.between(lastUpdateAt, observedAt).toMillis();
    assertTrue(elapsed <= 5_000, "Visibility must be within five seconds");
  }
}
