package com.loltracker.app.match;

import java.time.Instant;

public record MatchSummary(
    String matchId,
    String championName,
    boolean win,
    String gameMode,
    long durationSeconds,
    Instant gameEndAt) {}

