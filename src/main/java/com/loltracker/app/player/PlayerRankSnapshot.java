package com.loltracker.app.player;

public record PlayerRankSnapshot(
    String queueType,
    String tier,
    String division,
    int leaguePoints,
    int score) {

  public String displayName() {
    return "%s %s %d LP".formatted(formatTier(tier), division, leaguePoints);
  }

  private String formatTier(String value) {
    if (value == null || value.isBlank()) {
      return "Unranked";
    }
    String lower = value.toLowerCase();
    return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
  }
}
