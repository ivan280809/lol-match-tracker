package com.loltracker.app.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.loltracker.app.integration.telegram.TelegramDeliveryReceipt;
import com.loltracker.app.integration.telegram.TelegramNotifier;
import com.loltracker.app.match.TrackedMatchEntity;
import com.loltracker.app.player.PlayerEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock private TelegramNotifier telegramNotifier;
  @Mock private NotificationMessageFactory notificationMessageFactory;
  @Mock private NotificationStatsService notificationStatsService;
  @Mock private NotificationOutboxRepository notificationOutboxRepository;
  @Mock private NotificationDeliveryRecorder notificationDeliveryRecorder;

  @InjectMocks private NotificationService notificationService;

  @Test
  void enqueueMatchNotificationCreatesOutboxWhenMissing() {
    TrackedMatchEntity trackedMatch = trackedMatch();
    NotificationOutboxEntity saved = new NotificationOutboxEntity();
    saved.setId(10L);
    saved.setTrackedMatch(trackedMatch);

    when(notificationOutboxRepository.findByTrackedMatchId(9L)).thenReturn(Optional.empty());
    when(notificationOutboxRepository.save(org.mockito.ArgumentMatchers.any(NotificationOutboxEntity.class)))
        .thenReturn(saved);

    NotificationOutboxEntity result = notificationService.enqueueMatchNotification(trackedMatch);

    assertEquals(10L, result.getId());
    verify(notificationOutboxRepository).save(org.mockito.ArgumentMatchers.any(NotificationOutboxEntity.class));
  }

  @Test
  void dispatchPendingForPlayerBuildsMessageSendsItAndStoresReceipt() {
    PlayerEntity player = player();
    TrackedMatchEntity trackedMatch = trackedMatch();
    NotificationOutboxEntity outbox = outbox(trackedMatch);
    NotificationStatsSnapshot stats =
        new NotificationStatsSnapshot(
            "W", 1, "victoria", 1, 0, 1, 0, 1800, "Gold IV 10 LP", "Gold IV 10 LP", 0);
    when(notificationOutboxRepository
            .findTop50ByTrackedMatchPlayerIdAndStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.anyCollection(),
                org.mockito.ArgumentMatchers.any(Instant.class)))
        .thenReturn(List.of(outbox));
    when(notificationStatsService.buildFor(trackedMatch)).thenReturn(stats);
    when(notificationMessageFactory.build(trackedMatch, stats)).thenReturn("telegram-message");
    when(telegramNotifier.send("telegram-message")).thenReturn(new TelegramDeliveryReceipt(1234));

    NotificationDispatchResult result = notificationService.dispatchPendingForPlayer(player);

    assertEquals(1, result.sent());
    assertEquals(0, result.failed());
    verify(notificationStatsService).buildFor(trackedMatch);
    verify(notificationMessageFactory).build(trackedMatch, stats);
    verify(telegramNotifier).send("telegram-message");
    verify(notificationDeliveryRecorder).recordAttempt(outbox);
    verify(notificationDeliveryRecorder).recordSent(outbox, 1234);
  }

  @Test
  void dispatchPendingForPlayerRecordsFailureWithoutThrowing() {
    PlayerEntity player = player();
    TrackedMatchEntity trackedMatch = trackedMatch();
    NotificationOutboxEntity outbox = outbox(trackedMatch);
    when(notificationOutboxRepository
            .findTop50ByTrackedMatchPlayerIdAndStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.anyCollection(),
                org.mockito.ArgumentMatchers.any(Instant.class)))
        .thenReturn(List.of(outbox));
    when(notificationStatsService.buildFor(trackedMatch)).thenThrow(new IllegalStateException("Telegram down"));

    NotificationDispatchResult result = notificationService.dispatchPendingForPlayer(player);

    assertEquals(0, result.sent());
    assertEquals(1, result.failed());
    verify(notificationDeliveryRecorder).recordAttempt(outbox);
    verify(notificationDeliveryRecorder).recordFailure(
        org.mockito.ArgumentMatchers.eq(outbox), org.mockito.ArgumentMatchers.any(IllegalStateException.class));
    verifyNoInteractions(telegramNotifier);
  }

  private PlayerEntity player() {
    PlayerEntity player = new PlayerEntity();
    player.setId(7L);
    player.setGameName("Bazaga");
    player.setTagLine("ESP");
    return player;
  }

  private TrackedMatchEntity trackedMatch() {
    TrackedMatchEntity trackedMatch = new TrackedMatchEntity();
    trackedMatch.setId(9L);
    trackedMatch.setPlayer(player());
    trackedMatch.setMatchId("EUW1_900");
    trackedMatch.setChampionName("Lux");
    trackedMatch.setResult("VICTORY");
    trackedMatch.setGameMode("CLASSIC");
    trackedMatch.setDurationSeconds(1800);
    trackedMatch.setGameEndAt(Instant.parse("2026-04-03T18:00:00Z"));
    return trackedMatch;
  }

  private NotificationOutboxEntity outbox(TrackedMatchEntity trackedMatch) {
    NotificationOutboxEntity outbox = new NotificationOutboxEntity();
    outbox.setId(11L);
    outbox.setTrackedMatch(trackedMatch);
    outbox.setStatus(NotificationDeliveryStatus.PENDING);
    outbox.setNextAttemptAt(Instant.now());
    return outbox;
  }
}
