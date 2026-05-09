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
        .append("</b>\n");

    appendSummaryCard(message, match, queue);
    appendPerformanceSection(message, match);
    appendComparisonSection(message, stats);
    appendFormSection(message, match, stats);
    appendContextSection(message, stats);
    appendSharedSection(message, stats);
    appendHighlightsSection(message, stats);
    appendRankSection(message, stats);
    appendDetailsSection(message, match, queue);
    return message.toString();
  }

  private void appendSummaryCard(
      StringBuilder message, TrackedMatchEntity match, MatchQueueDescriptor queue) {
    message
        .append("<blockquote>")
        .append("<b>")
        .append(escape(formatChampion(match)))
        .append("</b> | ")
        .append(escape(queue.label()))
        .append(" | ")
        .append(escape(formatPosition(match)))
        .append("\n")
        .append("KDA ")
        .append(match.getKills())
        .append("/")
        .append(match.getDeaths())
        .append("/")
        .append(match.getAssists())
        .append(" (")
        .append(formatKdaRatio(match))
        .append(") | ")
        .append(formatDuration(match.getDurationSeconds()))
        .append("\n")
        .append(formatInstant(match))
        .append(" | ")
        .append(escape(formatServer(match)))
        .append("</blockquote>\n\n");
  }

  private void appendPerformanceSection(StringBuilder message, TrackedMatchEntity match) {
    message
        .append("<b>Rendimiento</b>\n")
        .append("<pre>");
    appendMetricRow(
        message,
        "KDA",
        match.getKills() + "/" + match.getDeaths() + "/" + match.getAssists(),
        formatKdaRatio(match));
    appendMetricRow(
        message,
        "CS",
        String.valueOf(match.getCreepScore()),
        formatOneDecimal(perMinute(match.getCreepScore(), match.getDurationSeconds())) + "/min");
    appendMetricRow(
        message,
        "Oro",
        formatCompactNumber(match.getGoldEarned()),
        formatWhole(perMinute(match.getGoldEarned(), match.getDurationSeconds())) + "/min");
    appendMetricRow(
        message,
        "Dano",
        formatCompactNumber(match.getDamageDealtToChampions()),
        formatWhole(perMinute(match.getDamageDealtToChampions(), match.getDurationSeconds()))
            + "/min");
    appendMetricRow(
        message,
        "Vision",
        String.valueOf(match.getVisionScore()),
        formatOneDecimal(perMinute(match.getVisionScore(), match.getDurationSeconds())) + "/min");
    message.append("</pre>\n\n");
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

  private void appendContextSection(StringBuilder message, NotificationStatsSnapshot stats) {
    if (!stats.recentProfile().hasData()
        && !stats.queueProfile().hasData()
        && !stats.championProfile().hasData()
        && !stats.positionProfile().hasData()) {
      return;
    }
    message.append("<b>Contexto</b>\n");
    appendProfileLine(message, stats.recentProfile());
    appendProfileLine(message, stats.queueProfile());
    appendProfileLine(message, stats.championProfile());
    appendProfileLine(message, stats.positionProfile());
    message.append("\n");
  }

  private void appendProfileLine(
      StringBuilder message, NotificationPerformanceProfile profile) {
    if (profile == null || !profile.hasData()) {
      return;
    }
    message
        .append(escape(profile.label()))
        .append(": <b>")
        .append(profile.wins())
        .append("W / ")
        .append(profile.losses())
        .append("L</b> (")
        .append(profile.winRate())
        .append("% WR, ")
        .append(profile.games())
        .append(" ")
        .append(pluralize(profile.games(), "partida", "partidas"))
        .append(") | KDA <code>")
        .append(formatOneDecimal(profile.averageKills()))
        .append("/")
        .append(formatOneDecimal(profile.averageDeaths()))
        .append("/")
        .append(formatOneDecimal(profile.averageAssists()))
        .append("</code> (<code>")
        .append(formatTwoDecimals(profile.averageKdaRatio()))
        .append("</code>)\n");
  }

  private void appendComparisonSection(StringBuilder message, NotificationStatsSnapshot stats) {
    NotificationPerformanceDelta delta = stats.performanceDelta();
    if (delta == null || !delta.available()) {
      return;
    }
    message
        .append("<b>Comparativa</b>\n")
        .append("Vs media reciente: KDA <code>")
        .append(formatSignedTwoDecimals(delta.kdaRatioDelta()))
        .append("</code> | CS/min <code>")
        .append(formatSignedOneDecimal(delta.csPerMinuteDelta()))
        .append("</code>\n")
        .append("Oro/min <code>")
        .append(formatSignedOneDecimal(delta.goldPerMinuteDelta()))
        .append("</code> | Dano/min <code>")
        .append(formatSignedOneDecimal(delta.damagePerMinuteDelta()))
        .append("</code> | Vision/min <code>")
        .append(formatSignedOneDecimal(delta.visionPerMinuteDelta()))
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
        .append("</b> | KDA grupo: <b>")
        .append(sharedKills(stats))
        .append("/")
        .append(sharedDeaths(stats))
        .append("/")
        .append(sharedAssists(stats))
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

  private void appendHighlightsSection(StringBuilder message, NotificationStatsSnapshot stats) {
    if (stats.highlights().isEmpty()) {
      return;
    }
    message.append("<b>Destacados</b>\n");
    stats
        .highlights()
        .forEach(highlight -> message.append("- ").append(escape(highlight)).append("\n"));
    message.append("\n");
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
        .append("</b>\n\n");
  }

  private void appendDetailsSection(
      StringBuilder message, TrackedMatchEntity match, MatchQueueDescriptor queue) {
    message
        .append("<blockquote expandable>")
        .append("<b>Detalles</b>\n")
        .append("Cola: ")
        .append(escape(formatQueue(queue)))
        .append("\n")
        .append("Servidor: ")
        .append(escape(formatServer(match)))
        .append("\n")
        .append("ID: ")
        .append(escape(formatText(match.getMatchId())))
        .append("</blockquote>");
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

  private void appendMetricRow(StringBuilder message, String label, String value, String detail) {
    message.append(escape(String.format(Locale.ROOT, "%-7s %-8s %s\n", label, value, detail)));
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

  private String formatSignedOneDecimal(double value) {
    return formatSigned(value, "%.1f");
  }

  private String formatSignedTwoDecimals(double value) {
    return formatSigned(value, "%.2f");
  }

  private String formatSigned(double value, String pattern) {
    String formatted = String.format(Locale.ROOT, pattern, value);
    return value > 0 ? "+" + formatted : formatted;
  }

  private String formatText(String value) {
    return value == null || value.isBlank() ? "Sin dato" : value;
  }

  private String normalizeLabel(String value) {
    return value == null ? "" : value.trim();
  }

  private String formatPlayerRank(NotificationStatsSnapshot stats) {
    String rankLabel =
        stats.playerRankQueueLabel() == null || stats.playerRankQueueLabel().isBlank()
            ? ""
            : " (" + escape(stats.playerRankQueueLabel()) + ")";
    String line = "Jugador" + rankLabel + ": <code>" + escape(stats.playerRank()) + "</code>";
    if (stats.playerRankNote() == null || stats.playerRankNote().isBlank()) {
      return line + "\n";
    }
    return line + " <i>" + escape(stats.playerRankNote()) + "</i>\n";
  }

  private boolean isVictory(TrackedMatchEntity match) {
    return "VICTORY".equalsIgnoreCase(match.getResult());
  }

  private int sharedKills(NotificationStatsSnapshot stats) {
    return stats.sharedPlayers().stream().mapToInt(NotificationSharedPlayer::kills).sum();
  }

  private int sharedDeaths(NotificationStatsSnapshot stats) {
    return stats.sharedPlayers().stream().mapToInt(NotificationSharedPlayer::deaths).sum();
  }

  private int sharedAssists(NotificationStatsSnapshot stats) {
    return stats.sharedPlayers().stream().mapToInt(NotificationSharedPlayer::assists).sum();
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

