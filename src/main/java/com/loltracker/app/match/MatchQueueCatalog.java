package com.loltracker.app.match;

import java.util.Map;

public final class MatchQueueCatalog {

  private static final Map<Integer, MatchQueueDescriptor> KNOWN_QUEUES =
      Map.ofEntries(
          Map.entry(0, new MatchQueueDescriptor(0, "Custom", MatchQueueType.CUSTOM)),
          Map.entry(400, new MatchQueueDescriptor(400, "Normal Draft", MatchQueueType.NORMAL_DRAFT)),
          Map.entry(420, new MatchQueueDescriptor(420, "Ranked Solo/Duo", MatchQueueType.RANKED_SOLO)),
          Map.entry(430, new MatchQueueDescriptor(430, "Normal Blind", MatchQueueType.NORMAL_BLIND)),
          Map.entry(440, new MatchQueueDescriptor(440, "Ranked Flex", MatchQueueType.RANKED_FLEX)),
          Map.entry(450, new MatchQueueDescriptor(450, "ARAM", MatchQueueType.ARAM)),
          Map.entry(490, new MatchQueueDescriptor(490, "Quickplay", MatchQueueType.QUICKPLAY)),
          Map.entry(700, new MatchQueueDescriptor(700, "Clash", MatchQueueType.OTHER)),
          Map.entry(830, new MatchQueueDescriptor(830, "Co-op vs AI Intro", MatchQueueType.BOT)),
          Map.entry(840, new MatchQueueDescriptor(840, "Co-op vs AI Beginner", MatchQueueType.BOT)),
          Map.entry(850, new MatchQueueDescriptor(850, "Co-op vs AI Intermediate", MatchQueueType.BOT)),
          Map.entry(1700, new MatchQueueDescriptor(1700, "Arena", MatchQueueType.ARENA)),
          Map.entry(1710, new MatchQueueDescriptor(1710, "Arena", MatchQueueType.ARENA)));

  private MatchQueueCatalog() {}

  public static MatchQueueDescriptor describe(Integer queueId, String gameMode) {
    if (queueId != null && KNOWN_QUEUES.containsKey(queueId)) {
      return KNOWN_QUEUES.get(queueId);
    }
    return new MatchQueueDescriptor(queueId, fallbackLabel(queueId, gameMode), MatchQueueType.OTHER);
  }

  private static String fallbackLabel(Integer queueId, String gameMode) {
    if (gameMode != null && !gameMode.isBlank()) {
      return titleCase(gameMode);
    }
    return queueId == null ? "Unknown queue" : "Queue " + queueId;
  }

  private static String titleCase(String value) {
    String normalized = value.trim().replace('_', ' ').toLowerCase();
    StringBuilder result = new StringBuilder(normalized.length());
    boolean capitalizeNext = true;
    for (char character : normalized.toCharArray()) {
      if (Character.isWhitespace(character)) {
        result.append(character);
        capitalizeNext = true;
      } else if (capitalizeNext) {
        result.append(Character.toUpperCase(character));
        capitalizeNext = false;
      } else {
        result.append(character);
      }
    }
    return result.toString();
  }
}
