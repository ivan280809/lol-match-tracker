package com.loltracker.app.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.times;

import com.loltracker.app.integration.telegram.TelegramDeliveryReceipt;
import com.loltracker.app.integration.telegram.TelegramNotifier;
import com.loltracker.app.match.TrackedMatchEntity;
import com.loltracker.app.player.PlayerEntity;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import org.springframework.web.client.RestClientResponseException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  private static final Instant RETRY_CUTOFF = Instant.parse("2026-10-05T00:00:00Z");

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
            "W",
            1,
            "victoria",
            1,
            0,
            1,
            0,
            1800,
            "Gold IV 10 LP",
            "actualizado ahora",
            "Gold IV 10 LP",
            0);
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

  @Test
  void stopsTheCurrentPlayerBatchImmediatelyAfterTelegram429() {
    PlayerEntity player = player();
    List<NotificationOutboxEntity> items = List.of(
        outbox(trackedMatch()), outbox(trackedMatch()), outbox(trackedMatch()));
    when(notificationOutboxRepository
            .findTop50ByTrackedMatchPlayerIdAndStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.anyCollection(),
                org.mockito.ArgumentMatchers.any(Instant.class)))
        .thenReturn(items);
    when(notificationMessageFactory.build(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn("message");
    RestClientResponseException rateLimit = new RestClientResponseException(
        "Telegram rate limited", 429, "Too Many Requests", new HttpHeaders(),
        "{\"parameters\":{\"retry_after\":120}}".getBytes(StandardCharsets.UTF_8),
        StandardCharsets.UTF_8);
    when(telegramNotifier.send("message")).thenThrow(rateLimit);

    NotificationDispatchResult result = notificationService.dispatchPendingForPlayer(player);

    assertEquals(0, result.sent());
    assertEquals(1, result.failed());
    assertEquals(true, result.rateLimited());
    verify(notificationDeliveryRecorder, times(1)).recordAttempt(org.mockito.ArgumentMatchers.any());
    verify(telegramNotifier, times(1)).send("message");
  }

  @Test
  void persistedTelegramCooldownBlocksNewlyQueuedNotifications() {
    NotificationOutboxEntity rateLimited = outbox(trackedMatch());
    rateLimited.setStatus(NotificationDeliveryStatus.FAILED);
    rateLimited.setLastError(NotificationDeliveryRecorder.TELEGRAM_RATE_LIMIT_ERROR_PREFIX + " HTTP 429");
    rateLimited.setNextAttemptAt(Instant.now().plusSeconds(120));
    when(notificationOutboxRepository.findTopByStatusAndLastErrorStartingWithOrderByNextAttemptAtDesc(
            NotificationDeliveryStatus.FAILED, NotificationDeliveryRecorder.TELEGRAM_RATE_LIMIT_ERROR_PREFIX))
        .thenReturn(Optional.of(rateLimited));

    NotificationDispatchResult result = notificationService.dispatchPendingForPlayer(player());

    assertEquals(NotificationDispatchResult.empty(), result);
    verify(notificationOutboxRepository, never())
        .findTop50ByTrackedMatchPlayerIdAndStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyCollection(),
            org.mockito.ArgumentMatchers.any(Instant.class));
    verifyNoInteractions(telegramNotifier);
  }

  @Test
  void dispatchPendingForPlayerHonorsRequestedMessageLimit() {
    PlayerEntity player = player();
    List<NotificationOutboxEntity> items =
        List.of(outbox(trackedMatch()), outbox(trackedMatch()), outbox(trackedMatch()));
    when(notificationOutboxRepository
            .findTop50ByTrackedMatchPlayerIdAndStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.anyCollection(),
                org.mockito.ArgumentMatchers.any(Instant.class)))
        .thenReturn(items);

    NotificationDispatchResult result = notificationService.dispatchPendingForPlayer(player, 2);

    assertEquals(2, result.sent());
    assertEquals(0, result.failed());
    org.mockito.Mockito.verify(notificationDeliveryRecorder, org.mockito.Mockito.times(2))
        .recordAttempt(org.mockito.ArgumentMatchers.any(NotificationOutboxEntity.class));
  }

  @Test
  void stopsStartingDeliveriesAfterDispatcherDeadline() {
    PlayerEntity player = player();
    List<NotificationOutboxEntity> items =
        List.of(outbox(trackedMatch()), outbox(trackedMatch()), outbox(trackedMatch()));
    when(notificationOutboxRepository
            .findTop50ByTrackedMatchPlayerIdAndStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.anyCollection(),
                org.mockito.ArgumentMatchers.any(Instant.class)))
        .thenReturn(items);
    doAnswer(
            invocation -> {
              Thread.sleep(30);
              return new TelegramDeliveryReceipt(1234);
            })
        .when(telegramNotifier)
        .send(nullable(String.class), org.mockito.ArgumentMatchers.any(java.time.Duration.class));

    long deadline = System.nanoTime() + 10_000_000L;
    NotificationDispatchResult result = notificationService.dispatchPendingForPlayer(player, 3, deadline);

    assertEquals(1, result.sent());
    assertEquals(0, result.failed());
    verify(telegramNotifier, times(1)).send(nullable(String.class), org.mockito.ArgumentMatchers.any(java.time.Duration.class));
  }

  @Test
  void selectsOnlyRetryStatusesThatAreDueAtTheCurrentClockTime() {
    PlayerEntity player = player();
    ReflectionTestUtils.setField(
        notificationService, "clock", Clock.fixed(RETRY_CUTOFF, ZoneOffset.UTC));

    NotificationDispatchResult result = notificationService.dispatchPendingForPlayer(player);

    assertEquals(0, result.sent());
    assertEquals(0, result.failed());
    org.mockito.ArgumentCaptor<java.util.Collection<NotificationDeliveryStatus>> statuses =
        org.mockito.ArgumentCaptor.forClass(java.util.Collection.class);
    verify(notificationOutboxRepository)
        .findTop50ByTrackedMatchPlayerIdAndStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            org.mockito.ArgumentMatchers.eq(7L),
            statuses.capture(),
            org.mockito.ArgumentMatchers.eq(RETRY_CUTOFF));
    org.junit.jupiter.api.Assertions.assertTrue(
        statuses.getValue().containsAll(
            List.of(NotificationDeliveryStatus.PENDING, NotificationDeliveryStatus.FAILED)));
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
