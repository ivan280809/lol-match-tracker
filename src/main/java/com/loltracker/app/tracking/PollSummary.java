package com.loltracker.app.tracking;

public record PollSummary(
    int playersProcessed,
    int newMatchesFound,
    int notificationsSent,
    int playerFailures,
    String status) {}
