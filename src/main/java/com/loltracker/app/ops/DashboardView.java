package com.loltracker.app.ops;

public record DashboardView(
    long totalPlayers,
    long totalMatches,
    long pendingNotifications,
    long failedNotifications) {

  public DashboardView(long totalPlayers, long totalMatches) {
    this(totalPlayers, totalMatches, 0, 0);
  }
}

