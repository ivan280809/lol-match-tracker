package com.loltracker.app.match;

import java.util.List;

public record PlayerRecentStatsView(
    String recentForm,
    int games,
    int wins,
    int losses,
    int winRate,
    long averageDurationSeconds,
    List<ChampionUsageView> favoriteChampions) {

  public static PlayerRecentStatsView empty() {
    return new PlayerRecentStatsView("Sin historial", 0, 0, 0, 0, 0, List.of());
  }
}
