package com.loltracker.app.notification;

import com.loltracker.app.match.TrackedMatchEntity;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageFactory {

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Europe/Madrid"));

  public String build(TrackedMatchEntity match, NotificationStatsSnapshot stats) {
    String player =
        escape(match.getPlayer().getGameName()) + "#" + escape(match.getPlayer().getTagLine());
    String result = isVictory(match) ? "VICTORIA" : "DERROTA";
    String resultMarker = isVictory(match) ? "[+]" : "[-]";

    return "<b>"
        + resultMarker
        + " "
        + result
        + " - "
        + player
        + "</b>\n"
        + "<i>"
        + escape(match.getChampionName())
        + " en "
        + escape(match.getGameMode())
        + "</i>\n\n"
        + "<b>Partida</b>\n"
        + "Duracion: <code>"
        + formatDuration(match.getDurationSeconds())
        + "</code>\n"
        + "Finalizada: <code>"
        + FORMATTER.format(match.getGameEndAt())
        + "</code>\n\n"
        + "<b>Forma reciente</b>\n"
        + "<code>"
        + escape(stats.recentForm())
        + "</code>\n"
        + "Racha: <b>"
        + stats.currentStreakCount()
        + " "
        + escape(formatStreakResult(stats))
        + "</b>\n"
        + "Ese dia: <b>"
        + stats.dayWins()
        + "W / "
        + stats.dayLosses()
        + "L</b>"
        + " ("
        + stats.dayTotal()
        + " "
        + pluralize(stats.dayTotal(), "partida", "partidas")
        + ")\n\n"
        + "<b>Con "
        + escape(match.getChampionName())
        + "</b>\n"
        + stats.championWins()
        + "W / "
        + stats.championLosses()
        + "L"
        + " - "
        + stats.championWinRate()
        + "% WR\n"
        + "Duracion media reciente: <code>"
        + formatDuration(stats.recentAverageDurationSeconds())
        + "</code>\n\n"
        + "<b>Rank</b>\n"
        + "Jugador: <code>"
        + escape(stats.playerRank())
        + "</code>\n"
        + "Media roster: <code>"
        + escape(stats.rosterAverageRank())
        + "</code>\n"
        + "Diferencia: <b>"
        + formatRankDelta(stats.rankDelta())
        + "</b>";
  }

  private String formatDuration(long seconds) {
    long minutes = seconds / 60;
    long remainingSeconds = seconds % 60;
    return minutes + "m " + remainingSeconds + "s";
  }

  private String formatStreakResult(NotificationStatsSnapshot stats) {
    return pluralize(stats.currentStreakCount(), stats.currentStreakResult(), stats.currentStreakResult() + "s");
  }

  private String pluralize(int count, String singular, String plural) {
    return count == 1 ? singular : plural;
  }

  private String formatRankDelta(Integer delta) {
    if (delta == null) {
      return "Sin datos";
    }
    if (delta > 0) {
      return "+" + delta;
    }
    return String.valueOf(delta);
  }

  private boolean isVictory(TrackedMatchEntity match) {
    return "VICTORY".equalsIgnoreCase(match.getResult());
  }

  private String escape(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}

