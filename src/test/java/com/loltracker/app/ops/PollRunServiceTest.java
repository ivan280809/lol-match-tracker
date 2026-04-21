package com.loltracker.app.ops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PollRunServiceTest {

  @Mock private PollRunRepository pollRunRepository;

  @InjectMocks private PollRunService pollRunService;

  @Test
  void startRunCreatesRunningEntity() {
    when(pollRunRepository.save(any(PollRunEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PollRunEntity run = pollRunService.startRun();

    assertEquals("RUNNING", run.getStatus());
    assertNotNull(run.getStartedAt());
  }

  @Test
  void completeRunMarksSuccessWhenThereAreNoErrors() {
    PollRunEntity run = new PollRunEntity();
    when(pollRunRepository.save(any(PollRunEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PollRunEntity completed = pollRunService.completeRun(run, 3, 2, 2, List.of());

    assertEquals("SUCCESS", completed.getStatus());
    assertEquals(3, completed.getPlayersProcessed());
    assertEquals(2, completed.getNewMatchesFound());
    assertEquals(2, completed.getNotificationsSent());
    assertNull(completed.getErrorSummary());
    assertNotNull(completed.getFinishedAt());
  }

  @Test
  void completeRunBuildsPartialSuccessSummary() {
    PollRunEntity run = new PollRunEntity();
    when(pollRunRepository.save(any(PollRunEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PollRunEntity completed =
        pollRunService.completeRun(
            run, 2, 1, 1, List.of("Bazaga#ESP: timeout", "LuxMain#EUW: rate limited"));

    assertEquals("PARTIAL_SUCCESS", completed.getStatus());
    assertEquals(
        "2 players failed: Bazaga#ESP: timeout; LuxMain#EUW: rate limited",
        completed.getErrorSummary());
  }

  @Test
  void failRunTruncatesLongErrorSummary() {
    PollRunEntity run = new PollRunEntity();
    when(pollRunRepository.save(any(PollRunEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PollRunEntity failed = pollRunService.failRun(run, 1, 0, 0, "x".repeat(600));

    assertEquals("ERROR", failed.getStatus());
    assertEquals(1, failed.getPlayersProcessed());
    assertEquals(500, failed.getErrorSummary().length());
    assertTrue(failed.getErrorSummary().chars().allMatch(ch -> ch == 'x'));
  }
}
