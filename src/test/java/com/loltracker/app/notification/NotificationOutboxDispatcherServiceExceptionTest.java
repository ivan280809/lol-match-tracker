package com.loltracker.app.notification;

import static org.mockito.Mockito.*;

import com.loltracker.app.player.PlayerEntity;
import com.loltracker.app.player.PlayerService;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Verifies that an exception during a player dispatch aborts the remainder of the dispatch
 * pass, preventing subsequent players from being processed.
 */
@ExtendWith(MockitoExtension.class)
class NotificationOutboxDispatcherServiceExceptionTest {

  @Mock private NotificationService notificationService;
  @Mock private PlayerService playerService;
  @InjectMocks private NotificationOutboxDispatcherService dispatcher;

  @Test
  void abortsRemainingPlayersWhenDispatchThrows() {
    PlayerEntity a = new PlayerEntity();
    a.setGameName("A");
    a.setTagLine("EUW");
    PlayerEntity b = new PlayerEntity();
    b.setGameName("B");
    b.setTagLine("EUW");
    PlayerEntity c = new PlayerEntity();
    c.setGameName("C");
    c.setTagLine("EUW");

    when(playerService.getActivePlayers()).thenReturn(List.of(a, b, c));
    // Player A succeeds with one sent message.
    when(notificationService.dispatchPendingForPlayer(eq(a), anyInt(), anyLong(), anyLong()))
        .thenReturn(new NotificationDispatchResult(1, 0, false));
    // Player B throws an exception to simulate a partial failure.
    when(notificationService.dispatchPendingForPlayer(eq(b), anyInt(), anyLong(), anyLong()))
        .thenThrow(new RuntimeException("dispatch error"));

    // Ensure the dispatcher has a large budget to cover all players if no exception occurs.
    ReflectionTestUtils.setField(dispatcher, "maxMessagesPerRun", 10);
    ReflectionTestUtils.setField(dispatcher, "maxDurationMillis", 90_000L);
    setDefaultRequestWindow(dispatcher);

    dispatcher.runDispatch();

    // Player A should be invoked.
    verify(notificationService).dispatchPendingForPlayer(eq(a), anyInt(), anyLong(), anyLong());
    // Player B should be invoked and then throw.
    verify(notificationService).dispatchPendingForPlayer(eq(b), anyInt(), anyLong(), anyLong());
    // Player C must not be invoked because the exception caused an early break.
    verify(notificationService, never()).dispatchPendingForPlayer(eq(c), anyInt(), anyLong(), anyLong());
  }

  private void setDefaultRequestWindow(NotificationOutboxDispatcherService target) {
    // HTTP timeout is derived from the remaining dispatch deadline per attempt.
  }
}
