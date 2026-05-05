package com.loltracker.app.integration.riot;

public record RiotMatchParticipant(
    String puuid,
    int championId,
    String championName,
    String lane,
    String role,
    int kills,
    int deaths,
    int assists,
    int creepScore,
    int goldEarned,
    int damageDealtToChampions,
    int visionScore,
    boolean win) {}
