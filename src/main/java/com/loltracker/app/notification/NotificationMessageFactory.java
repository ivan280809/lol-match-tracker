package com.loltracker.app.notification;

import com.loltracker.app.match.MatchQueueCatalog;
import com.loltracker.app.match.MatchQueueDescriptor;
import com.loltracker.app.match.TrackedMatchEntity;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageFactory {

  private final ZoneId appZoneId;

  public NotificationMessageFactory() {
    this(ZoneId.of("Europe/Madrid"));
  }

  @Autowired
  public NotificationMessageFactory(ZoneId appZoneId) {
    this.appZoneId = appZoneId;
  }

  public String build(TrackedMatchEntity match, NotificationStatsSnapshot stats) {
    String player =
        escape(match.getPlayer().getGameName()) + "#" + escape(match.getPlayer().getTagLine());
    String result = isVictory(match) ? "VICTORIA" : "DERROTA";
    String resultMarker = isVictory(match) ? "[+]" : "[-]";
    MatchQueueDescriptor queue = MatchQueueCatalog.describe(match.getQueueId(), match.getGameMode());

    StringBuilder message = new StringBuilder();
    message
        .append("<b>")
        .append(resultMarker)
        .append(" ")
        .append(result)
        .append(" | ")
        .append(player)
        .append("</b>\n")
        .append("<i>")
        .append(escape(formatChampion(match)))
        .append(" | ")
        .append(escape(queue.label()))
        .append(" | ")
        .append(escape(formatPosition(match)))
        .append("</i>\n\n");

    appendMatchSection(message, match, queue);
    appendPerformanceSection(message, match);
    appendFormSection(message, match, stats);
    appendSharedSection(message, stats);
    appendRankSection(message, stats);
    return message.toString();
  }

  private void appendMatchSection(
      StringBuilder message, TrackedMatchEntity match, MatchQueueDescriptor queue) {
    message
        .append("<b>Partida</b>\n")
        .append("Cola: <code>")
        .append(escape(formatQueue(queue)))
        .append("</code>\n")
        .append("Duracion: <code>")
        .append(formatDuration(match.getDurationSeconds()))
        .append("</code> | Fin: <code>")
        .append(formatInstant(match))
        .append("</code>\n")
        .append("Servidor: <code>")
        .append(escape(formatServer(match)))
        .append("</code>\n")
        .append("ID: <code>")
        .append(escape(formatText(match.getMatchId())))
        .append("</code>\n\n");
  }

  private void appendPerformanceSection(StringBuilder message, TrackedMatchEntity match) {
    message
        .append("<b>Rendimiento</b>\n")
        .append("KDA: <b>")
        .append(match.getKills())
        .append("/")
        .append(match.getDeaths())
        .append("/")
        .append(match.getAssists())
        .append("</b> (<code>")
        .append(formatKdaRatio(match))
        .append("</code>)\n")
        .append("CS: <code>")
        .append(match.getCreepScore())
        .append("</code> (<code>")
        .append(formatOneDecimal(perMinute(match.getCreepScore(), match.getDurationSeconds())))
        .append("/min</code>)\n")
        .append("Oro: <code>")
        .append(formatCompactNumber(match.getGoldEarned()))
        .append("</code> (<code>")
        .append(formatWhole(perMinute(match.getGoldEarned(), match.getDurationSeconds())))
        .append("/min</code>)\n")
        .append("Dano: <code>")
        .append(formatCompactNumber(match.getDamageDealtToChampions()))
        .append("</code> (<code>")
        .append(formatWhole(perMinute(match.getDamageDealtToChampions(), match.getDurationSeconds())))
        .append("/min</code>)\n")
        .append("Vision: <code>")
        .append(match.getVisionScore())
        .append("</code> (<code>")
        .append(formatOneDecimal(perMinute(match.getVisionScore(), match.getDurationSeconds())))
        .append("/min</code>)\n\n");
  }

  private void appendFormSection(
      StringBuilder message, TrackedMatchEntity match, NotificationStatsSnapshot stats) {
    message
        .append("<b>Forma</b>\n")
        .append("Reciente: <code>")
        .append(escape(stats.recentForm()))
        .append("</code>\n")
        .append("Racha: <b>")
        .append(stats.currentStreakCount())
        .append(" ")
        .append(escape(formatStreakResult(stats)))
        .append("</b>\n")
        .append("Ese dia: <b>")
        .append(stats.dayWins())
        .append("W / ")
        .append(stats.dayLosses())
        .append("L</b> (")
        .append(stats.dayTotal())
        .append(" ")
        .append(pluralize(stats.dayTotal(), "partida", "partidas"))
        .append(")\n")
        .append("Con ")
        .append(escape(match.getChampionName()))
        .append(": <b>")
        .append(stats.championWins())
        .append("W / ")
        .append(stats.championLosses())
        .append("L</b> (")
        .append(stats.championWinRate())
        .append("% WR)\n")
        .append("Duracion media: <code>")
        .append(formatDuration(stats.recentAverageDurationSeconds()))
        .append("</code>\n\n");
  }

  private void appendSharedSection(StringBuilder message, NotificationStatsSnapshot stats) {
    if (stats.sharedPlayers().isEmpty()) {
      return;
    }
    message
        .append("<b>Compartida</b>\n")
        .append("Tracked juntos: <b>")
        .append(stats.sharedPlayers().size())
        .append("</b>\n");
    stats.sharedPlayers().forEach(shared -> appendSharedPlayer(message, shared));
    message.append("\n");
  }

  private void appendSharedPlayer(StringBuilder message, NotificationSharedPlayer shared) {
    message
        .append("- ")
        .append(escape(shared.playerName()))
        .append(": ")
        .append(escape(shared.championName()))
        .append(" ")
        .append(shared.kills())
        .append("/")
        .append(shared.deaths())
        .append("/")
        .append(shared.assists())
        .append(" ")
        .append(escape(shared.result()))
        .append("\n");
  }

  private void appendRankSection(StringBuilder message, NotificationStatsSnapshot stats) {
    message
        .append("<b>Rank</b>\n")
        .append(formatPlayerRank(stats))
        .append("Media roster: <code>")
        .append(escape(stats.rosterAverageRank()))
        .append("</code>\n")
        .append("Diferencia: <b>")
        .append(formatRankDelta(stats.rankDelta()))
        .append("</b>");
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

  private String formatChampion(TrackedMatchEntity match) {
    if (match.getChampionId() == null || match.getChampionId() <= 0) {
      return formatText(match.getChampionName());
    }
    return formatText(match.getChampionName()) + " (#" + match.getChampionId() + ")";
  }

  private String formatQueue(MatchQueueDescriptor queue) {
    if (queue.queueId() == null) {
      return queue.label();
    }
    return queue.label() + " (" + queue.queueId() + ")";
  }

  private String formatPosition(TrackedMatchEntity match) {
    String lane = normalizeLabel(match.getLane());
    String role = normalizeLabel(match.getRole());
    if (lane.isBlank() && role.isBlank()) {
      return "Sin posicion";
    }
    if (lane.isBlank()) {
      return role;
    }
    if (role.isBlank() || role.equals(lane)) {
      return lane;
    }
    return lane + "/" + role;
  }

  private String formatServer(TrackedMatchEntity match) {
    String platform = normalizeLabel(match.getPlatform());
    String region = normalizeLabel(match.getRegion());
    if (platform.isBlank() && region.isBlank()) {
      return "Sin dato";
    }
    if (platform.isBlank()) {
      return region;
    }
    if (region.isBlank()) {
      return platform;
    }
    return platform + " / " + region;
  }

  private String formatInstant(TrackedMatchEntity match) {
    if (match.getGameEndAt() == null) {
      return "Sin fecha";
    }
    return formatter().format(match.getGameEndAt());
  }

  private String formatKdaRatio(TrackedMatchEntity match) {
    if (match.getDeaths() == 0) {
      return "Perfect";
    }
    double ratio = (match.getKills() + match.getAssists()) / (double) match.getDeaths();
    return formatTwoDecimals(ratio);
  }

  private double perMinute(long value, long durationSeconds) {
    if (durationSeconds <= 0) {
      return 0;
    }
    return value / (durationSeconds / 60.0);
  }

  private String formatCompactNumber(int value) {
    if (Math.abs(value) >= 1000) {
      return formatOneDecimal(value / 1000.0) + "k";
    }
    return String.valueOf(value);
  }

  private String formatWhole(double value) {
    return String.valueOf(Math.round(value));
  }

  private String formatOneDecimal(double value) {
    return String.format(Locale.ROOT, "%.1f", value);
  }

  private String formatTwoDecimals(double value) {
    return String.format(Locale.ROOT, "%.2f", value);
  }

  private String formatText(String value) {
    return value == null || value.isBlank() ? "Sin dato" : value;
  }

  private String normalizeLabel(String value) {
    return value == null ? "" : value.trim();
  }

  private String formatPlayerRank(NotificationStatsSnapshot stats) {
    String line = "Jugador: <code>" + escape(stats.playerRank()) + "</code>";
    if (stats.playerRankNote() == null || stats.playerRankNote().isBlank()) {
      return line + "\n";
    }
    return line + " <i>" + escape(stats.playerRankNote()) + "</i>\n";
  }

  private boolean isVictory(TrackedMatchEntity match) {
    return "VICTORY".equalsIgnoreCase(match.getResult());
  }

  private DateTimeFormatter formatter() {
    return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(appZoneId);
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

