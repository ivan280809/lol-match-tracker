package com.loltracker.app.match;

import java.time.Instant;

public record MatchSummary(
    String matchId,
    Integer championId,
    String championName,
    boolean win,
    String gameMode,
    Integer queueId,
    String lane,
    String role,
    int kills,
    int deaths,
    int assists,
    int creepScore,
    int goldEarned,
    int damageDealtToChampions,
    int visionScore,
    long durationSeconds,
    Instant gameEndAt,
    String platform,
    String region) {

  public MatchSummary(
      String matchId,
      String championName,
      boolean win,
      String gameMode,
      long durationSeconds,
      Instant gameEndAt) {
    this(
        matchId,
        null,
        championName,
        win,
        gameMode,
        null,
        "",
        "",
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        durationSeconds,
        gameEndAt,
        "",
        "");
  }
}

