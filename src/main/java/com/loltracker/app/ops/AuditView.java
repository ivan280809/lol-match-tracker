package com.loltracker.app.ops;

import com.loltracker.app.notification.NotificationOutboxView;
import com.loltracker.app.player.PlayerView;
import java.util.List;

public record AuditView(
    List<PlayerView> playerErrors,
    List<NotificationOutboxView> problemNotifications,
    List<PollRunView> pollRuns,
    List<PollRunView> rateLimitRuns,
    List<ExternalCallLogView> externalLogs) {}
