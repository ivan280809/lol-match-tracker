package com.loltracker.app.ops;

import java.time.Instant;

public record IntegrationHealthView(
    boolean databaseOk,
    String databaseSummary,
    boolean riotConfigured,
    boolean telegramConfigured,
    Instant lastRiotOkAt,
    Instant lastTelegramOkAt,
    long pendingNotifications,
    long failedNotifications) {}
