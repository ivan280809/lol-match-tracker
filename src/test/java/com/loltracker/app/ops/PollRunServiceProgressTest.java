package com.loltracker.app.ops;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import com.loltracker.app.player.PlayerEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollRunServiceProgressTest {
  @Mock
  private PollRunRepository pollRunRepository;
  @Mock private TaskScheduler taskScheduler;

  private MutableClock clock;

  @InjectMocks
  private PollRunService pollRunService;

  private PollRunEntity run;
  private List<PollRunEntity> savedEntities;

  @BeforeEach
  void setUp() {
    clock = new MutableClock(Instant.parse("2023-01-01T00:00:00Z"));
    pollRunService = new PollRunService(pollRunRepository, clock);
    run = new PollRunEntity();
    run.setId(1L);
    savedEntities = new ArrayList<>();
    run.setStatus("RUNNING");
    when(pollRunRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(run));
    lenient()
        .when(pollRunRepository.save(any(PollRunEntity.class)))
        .thenAnswer(invocation -> {
          PollRunEntity arg = invocation.getArgument(0);
          savedEntities.add(arg);
          return arg;
        });
  }

  @Test
  void groupsOneHundredPlayerProgressUpdatesIntoTwentyWrites() {
    for (int index = 0; index < 100; index++) {
      PlayerEntity player = new PlayerEntity();
      player.setGameName("player-" + index);
      player.setTagLine("EUW");
      pollRunService.updateProgress(run, player, "Procesando jugadores");
      clock.setInstant(clock.instant().plusSeconds(1));
    }
    assertEquals(20, savedEntities.size());
    verify(pollRunRepository, times(20)).findByIdForUpdate(1L);
    verify(pollRunRepository, times(20)).save(run);
  }

  @Test
  void testUpdateProgressFlushesOnlyOnStageChangeOrInterval() {
    pollRunService.updateProgress(run, null, "StageA");
    assertEquals(1, savedEntities.size());

    pollRunService.updateProgress(run, null, "StageA");
    assertEquals(1, savedEntities.size());
    verify(pollRunRepository, times(1)).findByIdForUpdate(1L);

    clock.setInstant(clock.instant().plusSeconds(5));
    pollRunService.updateProgress(run, null, "StageA");
    assertEquals(2, savedEntities.size());

    pollRunService.updateProgress(run, null, "StageB");
    assertEquals(3, savedEntities.size());
  }

  @Test
  void persistsFinalStateAndClearsBufferedProgressOnCompletion() {
    pollRunService.updateProgress(run, null, "StageA");
    clock.setInstant(clock.instant().plusSeconds(1));
    pollRunService.updateProgress(run, null, "StageA");

    pollRunService.completeRun(run, 1, 0, 0, List.of());
    assertEquals("Finalizado", run.getCurrentStage());
    assertEquals(2, savedEntities.size());

    pollRunService.updateProgress(run, null, "StageA");
    assertEquals(2, savedEntities.size());
    assertEquals("Finalizado", run.getCurrentStage());
  }

  @Test
  void scheduledFlushPersistsLatestPendingProgressAtFiveSecondDeadline() {
    ReflectionTestUtils.setField(pollRunService, "taskScheduler", taskScheduler);
    AtomicReference<Runnable> scheduled = new AtomicReference<>();
    when(taskScheduler.schedule(
            any(Runnable.class),
            any(Instant.class)))
        .thenAnswer(
            invocation -> {
              scheduled.set(invocation.getArgument(0));
              return null;
            });
    PlayerEntity first = new PlayerEntity();
    first.setGameName("first");
    first.setTagLine("EUW");
    PlayerEntity latest = new PlayerEntity();
    latest.setGameName("latest");
    latest.setTagLine("EUW");

    pollRunService.updateProgress(run, first, "StageA");
    clock.setInstant(clock.instant().plusSeconds(1));
    pollRunService.updateProgress(run, latest, "StageA");
    assertEquals(1, savedEntities.size());
    org.mockito.ArgumentCaptor<Instant> deadline =
        org.mockito.ArgumentCaptor.forClass(Instant.class);
    verify(taskScheduler)
        .schedule(any(Runnable.class), deadline.capture());
    assertEquals(Instant.parse("2023-01-01T00:00:04.500Z"), deadline.getValue());

    clock.setInstant(clock.instant().plusSeconds(4));
    scheduled.get().run();

    assertEquals(2, savedEntities.size());
    assertEquals("latest#EUW", run.getCurrentPlayer());
  }

  @Test
  void rollbackDoesNotAdvanceThePersistedProgressCache() {
    TransactionSynchronizationManager.initSynchronization();
    try {
      pollRunService.updateProgress(run, null, "StageA");
      assertEquals(1, savedEntities.size());
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }

    pollRunService.updateProgress(run, null, "StageA");

    assertEquals(2, savedEntities.size());
  }

  @Test
  void scheduledFlushRetriesAfterRepositoryFailureWithoutAnotherProgressUpdate() {
    ReflectionTestUtils.setField(pollRunService, "taskScheduler", taskScheduler);
    AtomicReference<Runnable> scheduled = new AtomicReference<>();
    AtomicReference<Instant> retryDeadline = new AtomicReference<>();
    when(taskScheduler.schedule(
            any(Runnable.class),
            any(Instant.class)))
        .thenAnswer(invocation -> {
          scheduled.set(invocation.getArgument(0));
          retryDeadline.set(invocation.getArgument(1));
          return null;
        });
    java.util.concurrent.atomic.AtomicInteger writes = new java.util.concurrent.atomic.AtomicInteger();
    doAnswer(invocation -> {
      PollRunEntity value = invocation.getArgument(0);
      if (writes.incrementAndGet() == 2) {
        throw new org.springframework.dao.DataAccessResourceFailureException("fixture database outage");
      }
      savedEntities.add(value);
      return value;
    }).when(pollRunRepository).save(any(PollRunEntity.class));

    pollRunService.updateProgress(run, null, "StageA");
    clock.setInstant(clock.instant().plusSeconds(1));
    pollRunService.updateProgress(run, null, "StageA");
    Runnable firstScheduledFlush = scheduled.get();

    clock.setInstant(clock.instant().plusSeconds(4));
    firstScheduledFlush.run();
    assertEquals(1, savedEntities.size());
    Runnable retry = scheduled.get();
    assertEquals(Instant.parse("2023-01-01T00:00:06Z"), retryDeadline.get());

    clock.setInstant(clock.instant().plusSeconds(1));
    retry.run();

    assertEquals(2, savedEntities.size());
    assertEquals("StageA", run.getCurrentStage());
  }

  @Test
  void scheduledFlushCannotOverwriteTerminalRunState() {
    ReflectionTestUtils.setField(pollRunService, "taskScheduler", taskScheduler);
    AtomicReference<Runnable> scheduled = new AtomicReference<>();
    when(taskScheduler.schedule(
            any(Runnable.class),
            any(Instant.class)))
        .thenAnswer(invocation -> {
          scheduled.set(invocation.getArgument(0));
          return null;
        });
    pollRunService.updateProgress(run, null, "StageA");
    clock.setInstant(clock.instant().plusSeconds(1));
    pollRunService.updateProgress(run, null, "StageA");

    run.setStatus("SUCCESS");
    run.setCurrentStage("Finalizado");
    clock.setInstant(clock.instant().plusSeconds(4));
    scheduled.get().run();

    assertEquals("SUCCESS", run.getStatus());
    assertEquals("Finalizado", run.getCurrentStage());
    verify(pollRunRepository, times(1)).save(run);
  }


  private static class MutableClock extends Clock {
    private Instant instant;

    MutableClock(Instant instant) {
      this.instant = instant;
    }

    void setInstant(Instant instant) {
      this.instant = instant;
    }

    @Override
    public Instant instant() {
      return instant;
    }

    @Override
    public ZoneOffset getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }
  }
  @Test
  void persistsFailureImmediatelyAndRejectsLaterBufferedProgress() {
    pollRunService.updateProgress(run, null, "StageA");
    pollRunService.failRun(run, 1, 0, 0, "fixture failure");

    assertEquals("ERROR", run.getStatus());
    assertEquals("Error", run.getCurrentStage());
    assertEquals(2, savedEntities.size());
    pollRunService.updateProgress(run, null, "StageB");
    assertEquals(2, savedEntities.size());
  }

  @Test
  void persistsRateLimitImmediatelyAndRejectsLaterBufferedProgress() {
    pollRunService.updateProgress(run, null, "StageA");
    pollRunService.rateLimited(run, 1, 0, 0, clock.instant().plusSeconds(30), "fixture rate limit");

    assertEquals("RATE_LIMITED", run.getStatus());
    assertEquals("Pausado por rate limit Riot", run.getCurrentStage());
    assertEquals(2, savedEntities.size());
    pollRunService.updateProgress(run, null, "StageB");
    assertEquals(2, savedEntities.size());
  }
}
