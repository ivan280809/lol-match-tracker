package com.loltracker.app.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.loltracker.app.match.TrackedMatchEntity;
import com.loltracker.app.player.PlayerEntity;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class NotificationMessageFactoryTest {

  private final NotificationMessageFactory notificationMessageFactory =
      new NotificationMessageFactory();

  @Test
  void buildFormatsTrackedMatchForTelegram() {
    PlayerEntity player = new PlayerEntity();
    player.setGameName("Bazaga");
    player.setTagLine("ESP");

    TrackedMatchEntity match = new TrackedMatchEntity();
    match.setPlayer(player);
    match.setChampionName("Lux");
    match.setResult("VICTORY");
    match.setGameMode("CLASSIC");
    match.setDurationSeconds(1800);
    match.setGameEndAt(Instant.parse("2026-04-03T18:00:00Z"));

    String message = notificationMessageFactory.build(match);

    assertEquals(
        "Jugador: Bazaga#ESP\n"
            + "Campeon: Lux\n"
            + "Resultado: VICTORY\n"
            + "Modo: CLASSIC\n"
            + "Duracion (s): 1800\n"
            + "Fin: 03/04/2026 20:00:00",
        message);
  }
}
