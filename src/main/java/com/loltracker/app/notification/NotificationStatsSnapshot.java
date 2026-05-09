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
    String playerRankQueueLabel,
    String rosterAverageRank,
    Integer rankDelta,
    List<NotificationSharedPlayer> sharedPlayers,
    NotificationPerformanceProfile recentProfile,
    NotificationPerformanceProfile queueProfile,
    NotificationPerformanceProfile championProfile,
    NotificationPerformanceProfile positionProfile,
    NotificationPerformanceDelta performanceDelta,
    List<String> highlights) {

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
        "",
        rosterAverageRank,
        rankDelta,
        List.of(),
        NotificationPerformanceProfile.empty("Ultimas"),
        NotificationPerformanceProfile.empty("Cola"),
        NotificationPerformanceProfile.empty("Campeon"),
        NotificationPerformanceProfile.empty("Posicion"),
        NotificationPerformanceDelta.empty(),
        List.of());
  }

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
      String playerRankQueueLabel,
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
        playerRankQueueLabel,
        rosterAverageRank,
        rankDelta,
        List.of(),
        NotificationPerformanceProfile.empty("Ultimas"),
        NotificationPerformanceProfile.empty("Cola"),
        NotificationPerformanceProfile.empty("Campeon"),
        NotificationPerformanceProfile.empty("Posicion"),
        NotificationPerformanceDelta.empty(),
        List.of());
  }

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
      String playerRankQueueLabel,
      String rosterAverageRank,
      Integer rankDelta,
      List<NotificationSharedPlayer> sharedPlayers) {
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
        playerRankQueueLabel,
        rosterAverageRank,
        rankDelta,
        sharedPlayers,
        NotificationPerformanceProfile.empty("Ultimas"),
        NotificationPerformanceProfile.empty("Cola"),
        NotificationPerformanceProfile.empty("Campeon"),
        NotificationPerformanceProfile.empty("Posicion"),
        NotificationPerformanceDelta.empty(),
        List.of());
  }

  public NotificationStatsSnapshot {
    playerRankQueueLabel = playerRankQueueLabel == null ? "" : playerRankQueueLabel;
    sharedPlayers = sharedPlayers == null ? List.of() : List.copyOf(sharedPlayers);
    recentProfile =
        recentProfile == null ? NotificationPerformanceProfile.empty("Ultimas") : recentProfile;
    queueProfile = queueProfile == null ? NotificationPerformanceProfile.empty("Cola") : queueProfile;
    championProfile =
        championProfile == null ? NotificationPerformanceProfile.empty("Campeon") : championProfile;
    positionProfile =
        positionProfile == null ? NotificationPerformanceProfile.empty("Posicion") : positionProfile;
    performanceDelta =
        performanceDelta == null ? NotificationPerformanceDelta.empty() : performanceDelta;
    highlights = highlights == null ? List.of() : List.copyOf(highlights);
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
