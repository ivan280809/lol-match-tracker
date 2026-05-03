package com.loltracker.app.notification;

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
    String rosterAverageRank,
    Integer rankDelta) {

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
