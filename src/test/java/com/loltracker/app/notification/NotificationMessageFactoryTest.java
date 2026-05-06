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

    NotificationStatsSnapshot stats =
        new NotificationStatsSnapshot(
            "W W L",
            2,
            "victoria",
            2,
            1,
            4,
            2,
            1650,
            "Gold II 43 LP",
            "actualizado ahora",
            "Silver I 78 LP",
            165);

    String message = notificationMessageFactory.build(match, stats);

    assertEquals(
        "<b>[+] VICTORIA - Bazaga#ESP</b>\n"
            + "<i>Lux en CLASSIC</i>\n\n"
            + "<b>Partida</b>\n"
            + "Duracion: <code>30m 0s</code>\n"
            + "Finalizada: <code>03/04/2026 20:00</code>\n\n"
            + "<b>Forma reciente</b>\n"
            + "<code>W W L</code>\n"
            + "Racha: <b>2 victorias</b>\n"
            + "Ese dia: <b>2W / 1L</b> (3 partidas)\n\n"
            + "<b>Con Lux</b>\n"
            + "4W / 2L - 67% WR\n"
            + "Duracion media reciente: <code>27m 30s</code>\n\n"
            + "<b>Rank</b>\n"
            + "Jugador: <code>Gold II 43 LP</code> <i>actualizado ahora</i>\n"
            + "Media roster: <code>Silver I 78 LP</code>\n"
            + "Diferencia: <b>+165</b>",
        message);
  }

  @Test
  void buildEscapesTelegramHtml() {
    PlayerEntity player = new PlayerEntity();
    player.setGameName("Baza<ga");
    player.setTagLine("E&SP");

    TrackedMatchEntity match = new TrackedMatchEntity();
    match.setPlayer(player);
    match.setChampionName("Lux > Morgana");
    match.setResult("DEFEAT");
    match.setGameMode("CLASSIC");
    match.setDurationSeconds(65);
    match.setGameEndAt(Instant.parse("2026-04-03T18:00:00Z"));

    NotificationStatsSnapshot stats =
        new NotificationStatsSnapshot(
            "W < L",
            1,
            "derrota",
            0,
            1,
            0,
            1,
            65,
            "Sin < rank",
            "guardado & pendiente < retry",
            "Media & roster",
            null);

    String message = notificationMessageFactory.build(match, stats);

    assertEquals(
        "<b>[-] DERROTA - Baza&lt;ga#E&amp;SP</b>\n"
            + "<i>Lux &gt; Morgana en CLASSIC</i>\n\n"
            + "<b>Partida</b>\n"
            + "Duracion: <code>1m 5s</code>\n"
            + "Finalizada: <code>03/04/2026 20:00</code>\n\n"
            + "<b>Forma reciente</b>\n"
            + "<code>W &lt; L</code>\n"
            + "Racha: <b>1 derrota</b>\n"
            + "Ese dia: <b>0W / 1L</b> (1 partida)\n\n"
            + "<b>Con Lux &gt; Morgana</b>\n"
            + "0W / 1L - 0% WR\n"
            + "Duracion media reciente: <code>1m 5s</code>\n\n"
            + "<b>Rank</b>\n"
            + "Jugador: <code>Sin &lt; rank</code> <i>guardado &amp; pendiente &lt; retry</i>\n"
            + "Media roster: <code>Media &amp; roster</code>\n"
            + "Diferencia: <b>Sin datos</b>",
        message);
  }
}
