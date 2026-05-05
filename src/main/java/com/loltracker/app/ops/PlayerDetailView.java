package com.loltracker.app.ops;

import com.loltracker.app.match.PlayerRecentStatsView;
import com.loltracker.app.match.TrackedMatchView;
import com.loltracker.app.notification.NotificationOutboxView;
import com.loltracker.app.player.PlayerView;
import java.util.List;

public record PlayerDetailView(
    PlayerView player,
    List<TrackedMatchView> recentMatches,
    PlayerRecentStatsView stats,
    List<NotificationOutboxView> notificationWarnings) {}
