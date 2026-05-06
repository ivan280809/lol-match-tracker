package com.loltracker.app.match;

import java.time.Instant;

public record SharedMatchStatsView(
    String matchId,
    Integer queueId,
    String queueLabel,
    MatchQueueType queueType,
    Instant gameEndAt,
    int trackedPlayers,
    int wins,
    int losses,
    double averageKills,
    double averageDeaths,
    double averageAssists) {}
