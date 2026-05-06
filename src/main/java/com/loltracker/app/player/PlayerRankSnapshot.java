package com.loltracker.app.player;

public record PlayerRankSnapshot(
    String queueType,
    String queueLabel,
    String tier,
    String division,
    int leaguePoints,
    int score) {

  public PlayerRankSnapshot(
      String queueType, String tier, String division, int leaguePoints, int score) {
    this(queueType, queueLabel(queueType), tier, division, leaguePoints, score);
  }

  public String displayName() {
    return "%s %s %d LP".formatted(formatTier(tier), division, leaguePoints);
  }

  public String displayQueueLabel() {
    if (queueLabel != null && !queueLabel.isBlank()) {
      return queueLabel;
    }
    return queueLabel(queueType);
  }

  public String displayNameWithQueue() {
    return displayQueueLabel() + ": " + displayName();
  }

  public static String queueLabel(String queueType) {
    if ("RANKED_SOLO_5x5".equals(queueType)) {
      return "Solo/Duo";
    }
    if ("RANKED_FLEX_SR".equals(queueType)) {
      return "Flex";
    }
    if ("ROSTER_AVERAGE".equals(queueType)) {
      return "Roster";
    }
    return "Rank";
  }

  private String formatTier(String value) {
    if (value == null || value.isBlank()) {
      return "Unranked";
    }
    String lower = value.toLowerCase();
    return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
  }
}
