package com.loltracker.app.integration.riot;

public record RiotRankEntry(
    String queueType,
    String tier,
    String rank,
    int leaguePoints,
    int wins,
    int losses) {}
