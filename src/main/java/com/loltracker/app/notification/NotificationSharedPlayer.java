package com.loltracker.app.notification;

public record NotificationSharedPlayer(
    String playerName,
    String championName,
    String result,
    int kills,
    int deaths,
    int assists) {}
