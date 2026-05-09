package com.loltracker.app.notification;

public record NotificationPerformanceProfile(
    String label,
    int games,
    int wins,
    int losses,
    int winRate,
    double averageKills,
    double averageDeaths,
    double averageAssists,
    double averageKdaRatio,
    double averageCsPerMinute,
    double averageGoldPerMinute,
    double averageDamagePerMinute,
    double averageVisionPerMinute) {

  public static NotificationPerformanceProfile empty(String label) {
    return new NotificationPerformanceProfile(label, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
  }

  public boolean hasData() {
    return games > 0;
  }
}
