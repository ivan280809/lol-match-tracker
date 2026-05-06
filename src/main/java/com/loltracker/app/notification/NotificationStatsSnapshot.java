package com.loltracker.app.notification;

import java.util.List;

public record NotificationStatsSnapshot(
    String recentForm,
    int currentStreakCount,
    String currentStreakResult,
    int dayWins,
    int dayLosses,
    int championWins,
    int championLosses,
    long recentAverageDurationSeconds,
    String playerRank,
    String playerRankNote,
    String rosterAverageRank,
    Integer rankDelta,
    List<NotificationSharedPlayer> sharedPlayers) {

  public NotificationStatsSnapshot(
      String recentForm,
      int currentStreakCount,
      String currentStreakResult,
      int dayWins,
      int dayLosses,
      int championWins,
      int championLosses,
      long recentAverageDurationSeconds,
      String playerRank,
      String playerRankNote,
      String rosterAverageRank,
      Integer rankDelta) {
    this(
        recentForm,
        currentStreakCount,
        currentStreakResult,
        dayWins,
        dayLosses,
        championWins,
        championLosses,
        recentAverageDurationSeconds,
        playerRank,
        playerRankNote,
        rosterAverageRank,
        rankDelta,
        List.of());
  }

  public NotificationStatsSnapshot {
    sharedPlayers = sharedPlayers == null ? List.of() : List.copyOf(sharedPlayers);
  }

  public int dayTotal() {
    return dayWins + dayLosses;
  }

  public int championTotal() {
    return championWins + championLosses;
  }

  public int championWinRate() {
    return percentage(championWins, championTotal());
  }

  private static int percentage(int value, int total) {
    if (total == 0) {
      return 0;
    }
    return Math.round((value * 100.0f) / total);
  }
}
