package com.loltracker.app.notification;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.loltracker.app.player.PlayerEntity;
import com.loltracker.app.player.PlayerService;
import java.util.List;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationOutboxDispatcherServiceTest {

  @Mock private NotificationService notificationService;
  @Mock private PlayerService playerService;
  @InjectMocks private NotificationOutboxDispatcherService dispatcher;

  @Test
  void appliesOneMessageBudgetAcrossPlayersIncludingFailures() {
    PlayerEntity first = new PlayerEntity();
    first.setGameName("first");
    first.setTagLine("EUW");
    PlayerEntity second = new PlayerEntity();
    second.setGameName("second");
    second.setTagLine("EUW");
    when(playerService.getActivePlayers()).thenReturn(List.of(first, second));
    when(notificationService.dispatchPendingForPlayer(eq(first), eq(2), anyLong(), anyLong()))
        .thenReturn(new NotificationDispatchResult(1, 1));
    ReflectionTestUtils.setField(dispatcher, "maxMessagesPerRun", 2);
    ReflectionTestUtils.setField(dispatcher, "maxDurationMillis", 90_000L);
    setDefaultRequestWindow(dispatcher);

    dispatcher.runDispatch();

    verify(notificationService).dispatchPendingForPlayer(eq(first), eq(2), anyLong(), anyLong());
    verifyNoMoreInteractions(notificationService);
  }

  @Test
  void stopsDispatchingOtherPlayersAfterA429CooldownIsRecorded() {
    PlayerEntity first = new PlayerEntity();
    first.setGameName("first");
    first.setTagLine("EUW");
    PlayerEntity second = new PlayerEntity();
    second.setGameName("second");
    second.setTagLine("EUW");
    when(playerService.getActivePlayers()).thenReturn(List.of(first, second));
    when(notificationService.dispatchPendingForPlayer(eq(first), eq(2), anyLong(), anyLong()))
        .thenReturn(new NotificationDispatchResult(0, 1, true));
    ReflectionTestUtils.setField(dispatcher, "maxMessagesPerRun", 2);
    ReflectionTestUtils.setField(dispatcher, "maxDurationMillis", 90_000L);
    setDefaultRequestWindow(dispatcher);

    dispatcher.runDispatch();

    verify(notificationService).dispatchPendingForPlayer(eq(first), eq(2), anyLong(), anyLong());
    verifyNoMoreInteractions(notificationService);
  }

  @Test
  void zeroBudgetDoesNotReadPlayersOrDispatch() {
    ReflectionTestUtils.setField(dispatcher, "maxMessagesPerRun", 0);
    ReflectionTestUtils.setField(dispatcher, "maxDurationMillis", 10_000L);

    dispatcher.runDispatch();

    verifyNoInteractions(playerService, notificationService);
  }

  @Test
  void startsDispatchWithinConfiguredRunDeadlineEvenWhenItIsShorterThanDefaultHttpWindow() {
    PlayerEntity first = new PlayerEntity();
    first.setGameName("first");
    first.setTagLine("EUW");
    when(playerService.getActivePlayers()).thenReturn(List.of(first));
    ReflectionTestUtils.setField(dispatcher, "maxMessagesPerRun", 100);
    ReflectionTestUtils.setField(dispatcher, "maxDurationMillis", 5L);
    setDefaultRequestWindow(dispatcher);

    dispatcher.runDispatch();

    verify(notificationService)
        .dispatchPendingForPlayer(eq(first), eq(100), anyLong(), eq(0L));
  }

  @Test
  void passesDispatcherDeadlineWithoutReservingAnUnboundedWorstCaseWindow() {
    PlayerEntity player = new PlayerEntity();
    player.setGameName("player");
    player.setTagLine("EUW");
    when(playerService.getActivePlayers()).thenReturn(List.of(player));
    when(notificationService.dispatchPendingForPlayer(eq(player), eq(1), anyLong(), anyLong()))
        .thenReturn(NotificationDispatchResult.empty());
    ReflectionTestUtils.setField(dispatcher, "maxMessagesPerRun", 1);
    ReflectionTestUtils.setField(dispatcher, "maxDurationMillis", 90_000L);
    setDefaultRequestWindow(dispatcher);

    dispatcher.runDispatch();

    verify(notificationService)
        .dispatchPendingForPlayer(eq(player), eq(1), anyLong(), eq(0L));
  }

  @Test
  void concurrentLocalDispatchInvocationCannotClaimTheSameOutboxTwice() throws Exception {
    PlayerEntity player = new PlayerEntity();
    player.setGameName("player");
    player.setTagLine("EUW");
    CountDownLatch firstRunStarted = new CountDownLatch(1);
    CountDownLatch releaseFirstRun = new CountDownLatch(1);
    when(playerService.getActivePlayers()).thenReturn(List.of(player));
    when(notificationService.dispatchPendingForPlayer(eq(player), eq(1), anyLong(), anyLong()))
        .thenAnswer(
            invocation -> {
              firstRunStarted.countDown();
              assertTrue(releaseFirstRun.await(2, TimeUnit.SECONDS));
              return NotificationDispatchResult.empty();
            });
    ReflectionTestUtils.setField(dispatcher, "maxMessagesPerRun", 1);
    ReflectionTestUtils.setField(dispatcher, "maxDurationMillis", 90_000L);
    setDefaultRequestWindow(dispatcher);

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      var firstRun = executor.submit(dispatcher::runDispatch);
      assertTrue(firstRunStarted.await(2, TimeUnit.SECONDS));
      dispatcher.runDispatch();
      releaseFirstRun.countDown();
      firstRun.get(2, TimeUnit.SECONDS);
    } finally {
      executor.shutdownNow();
    }

    verify(playerService, times(1)).getActivePlayers();
    verify(notificationService, times(1))
        .dispatchPendingForPlayer(eq(player), eq(1), anyLong(), anyLong());
  }

  private void setDefaultRequestWindow(NotificationOutboxDispatcherService target) {
    // HTTP timeout is derived from the remaining dispatch deadline per attempt.
  }
}
