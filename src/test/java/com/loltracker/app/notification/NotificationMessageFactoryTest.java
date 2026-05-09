package com.loltracker.app.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.loltracker.app.match.TrackedMatchEntity;
import com.loltracker.app.player.PlayerEntity;
import java.time.Instant;
import java.util.List;
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
    match.setMatchId("EUW1_123");
    match.setChampionId(99);
    match.setChampionName("Lux");
    match.setResult("VICTORY");
    match.setGameMode("CLASSIC");
    match.setQueueId(420);
    match.setLane("MID");
    match.setRole("SOLO");
    match.setKills(8);
    match.setDeaths(3);
    match.setAssists(11);
    match.setCreepScore(210);
    match.setGoldEarned(12345);
    match.setDamageDealtToChampions(22000);
    match.setVisionScore(32);
    match.setDurationSeconds(1800);
    match.setGameEndAt(Instant.parse("2026-04-03T18:00:00Z"));
    match.setPlatform("EUW1");
    match.setRegion("EUROPE");

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
            "Solo/Duo",
            "Silver I 78 LP",
            165);

    String message = notificationMessageFactory.build(match, stats);

    assertEquals(
        "<b>[+] VICTORIA | Bazaga#ESP</b>\n"
            + "<blockquote><b>Lux (#99)</b> | Ranked Solo/Duo | MID/SOLO\n"
            + "KDA 8/3/11 (6.33) | 30m 0s\n"
            + "03/04/2026 20:00 | EUW1 / EUROPE</blockquote>\n\n"
            + "<b>Rendimiento</b>\n"
            + "<pre>KDA     8/3/11   6.33\n"
            + "CS      210      7.0/min\n"
            + "Oro     12.3k    412/min\n"
            + "Dano    22.0k    733/min\n"
            + "Vision  32       1.1/min\n"
            + "</pre>\n\n"
            + "<b>Forma</b>\n"
            + "Reciente: <code>W W L</code>\n"
            + "Racha: <b>2 victorias</b>\n"
            + "Ese dia: <b>2W / 1L</b> (3 partidas)\n"
            + "Con Lux: <b>4W / 2L</b> (67% WR)\n"
            + "Duracion media: <code>27m 30s</code>\n\n"
            + "<b>Rank</b>\n"
            + "Jugador (Solo/Duo): <code>Gold II 43 LP</code> <i>actualizado ahora</i>\n"
            + "Media roster: <code>Silver I 78 LP</code>\n"
            + "Diferencia: <b>+165</b>\n\n"
            + "<blockquote expandable><b>Detalles</b>\n"
            + "Cola: Ranked Solo/Duo (420)\n"
            + "Servidor: EUW1 / EUROPE\n"
            + "ID: EUW1_123</blockquote>",
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
        "<b>[-] DERROTA | Baza&lt;ga#E&amp;SP</b>\n"
            + "<blockquote><b>Lux &gt; Morgana</b> | Classic | Sin posicion\n"
            + "KDA 0/0/0 (Perfect) | 1m 5s\n"
            + "03/04/2026 20:00 | Sin dato</blockquote>\n\n"
            + "<b>Rendimiento</b>\n"
            + "<pre>KDA     0/0/0    Perfect\n"
            + "CS      0        0.0/min\n"
            + "Oro     0        0/min\n"
            + "Dano    0        0/min\n"
            + "Vision  0        0.0/min\n"
            + "</pre>\n\n"
            + "<b>Forma</b>\n"
            + "Reciente: <code>W &lt; L</code>\n"
            + "Racha: <b>1 derrota</b>\n"
            + "Ese dia: <b>0W / 1L</b> (1 partida)\n"
            + "Con Lux &gt; Morgana: <b>0W / 1L</b> (0% WR)\n"
            + "Duracion media: <code>1m 5s</code>\n\n"
            + "<b>Rank</b>\n"
            + "Jugador: <code>Sin &lt; rank</code> <i>guardado &amp; pendiente &lt; retry</i>\n"
            + "Media roster: <code>Media &amp; roster</code>\n"
            + "Diferencia: <b>Sin datos</b>\n\n"
            + "<blockquote expandable><b>Detalles</b>\n"
            + "Cola: Classic\n"
            + "Servidor: Sin dato\n"
            + "ID: Sin dato</blockquote>",
        message);
  }

  @Test
  void buildIncludesSharedTrackedPlayersWhenPresent() {
    PlayerEntity player = new PlayerEntity();
    player.setGameName("Bazaga");
    player.setTagLine("ESP");

    TrackedMatchEntity match = new TrackedMatchEntity();
    match.setPlayer(player);
    match.setMatchId("EUW1_123");
    match.setChampionName("Lux");
    match.setResult("VICTORY");
    match.setGameMode("CLASSIC");
    match.setDurationSeconds(1800);
    match.setGameEndAt(Instant.parse("2026-04-03T18:00:00Z"));

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
            "guardado",
            "Solo/Duo",
            "Gold IV 10 LP",
            0,
            List.of(
                new NotificationSharedPlayer("Bazaga#ESP", "Lux", "W", 8, 3, 11),
                new NotificationSharedPlayer("Duo<One#EUW", "Ahri & Zoe", "W", 4, 5, 9)));

    String message = notificationMessageFactory.build(match, stats);

    org.junit.jupiter.api.Assertions.assertTrue(message.contains("<b>Compartida</b>\n"));
    org.junit.jupiter.api.Assertions.assertTrue(
        message.contains("Tracked juntos: <b>2</b> | KDA grupo: <b>12/8/20</b>\n"));
    org.junit.jupiter.api.Assertions.assertTrue(message.contains("- Bazaga#ESP: Lux 8/3/11 W\n"));
    org.junit.jupiter.api.Assertions.assertTrue(
        message.contains("- Duo&lt;One#EUW: Ahri &amp; Zoe 4/5/9 W\n"));
  }

  @Test
  void buildIncludesDerivedContextComparisonAndHighlights() {
    PlayerEntity player = new PlayerEntity();
    player.setGameName("Bazaga");
    player.setTagLine("ESP");

    TrackedMatchEntity match = new TrackedMatchEntity();
    match.setPlayer(player);
    match.setMatchId("EUW1_123");
    match.setChampionName("Lux");
    match.setResult("VICTORY");
    match.setGameMode("CLASSIC");
    match.setQueueId(420);
    match.setLane("MID");
    match.setRole("SOLO");
    match.setKills(12);
    match.setDeaths(0);
    match.setAssists(8);
    match.setDurationSeconds(1800);
    match.setGameEndAt(Instant.parse("2026-04-03T18:00:00Z"));

    NotificationStatsSnapshot stats =
        new NotificationStatsSnapshot(
            "W W W",
            3,
            "victoria",
            1,
            0,
            3,
            0,
            1800,
            "Gold II 43 LP",
            "actualizado ahora",
            "Solo/Duo",
            "Silver I 78 LP",
            165,
            List.of(),
            new NotificationPerformanceProfile(
                "Ult. 30", 5, 3, 2, 60, 6.2, 3.4, 8.0, 4.18, 6.4, 395.0, 620.0, 0.8),
            new NotificationPerformanceProfile(
                "Solo/Duo", 4, 3, 1, 75, 7.0, 3.0, 8.5, 5.17, 6.8, 410.0, 650.0, 0.9),
            new NotificationPerformanceProfile(
                "Lux", 3, 3, 0, 100, 8.0, 2.0, 9.0, 8.5, 7.0, 430.0, 700.0, 1.0),
            new NotificationPerformanceProfile(
                "MID/SOLO", 3, 2, 1, 67, 7.3, 3.7, 8.3, 4.2, 6.9, 405.0, 630.0, 0.8),
            new NotificationPerformanceDelta(true, 15.82, 0.6, 25.0, 113.0, 0.3),
            List.of("KDA perfecto", "3 victorias seguidas", "Lux fuerte: 100% WR"));

    String message = notificationMessageFactory.build(match, stats);

    org.junit.jupiter.api.Assertions.assertTrue(message.contains("<b>Comparativa</b>\n"));
    org.junit.jupiter.api.Assertions.assertTrue(
        message.contains("Vs media reciente: KDA <code>+15.82</code> | CS/min <code>+0.6</code>\n"));
    org.junit.jupiter.api.Assertions.assertTrue(message.contains("<b>Contexto</b>\n"));
    org.junit.jupiter.api.Assertions.assertTrue(
        message.contains("Ult. 30: <b>3W / 2L</b> (60% WR, 5 partidas)"));
    org.junit.jupiter.api.Assertions.assertTrue(
        message.contains("Lux: <b>3W / 0L</b> (100% WR, 3 partidas)"));
    org.junit.jupiter.api.Assertions.assertTrue(message.contains("<b>Destacados</b>\n"));
    org.junit.jupiter.api.Assertions.assertTrue(message.contains("- KDA perfecto\n"));
  }
}
