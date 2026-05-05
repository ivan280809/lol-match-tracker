package com.loltracker.app.player;

import java.time.Instant;

public record PlayerView(
    Long id,
    RiotPlatform platform,
    String gameName,
    String tagLine,
    String puuid,
    boolean active,
    Instant archivedAt,
    Instant lastPolledAt,
    Instant trackFrom,
    PlayerBackfillMode backfillMode,
    Instant lastSuccessfulSyncAt,
    String lastSyncStatus,
    String lastError,
    String rankQueueType,
    String rankTier,
    String rankDivision,
    Integer rankLeaguePoints,
    Integer rankScore,
    Instant rankUpdatedAt) {

  public PlayerView(
      Long id,
      String gameName,
      String tagLine,
      String puuid,
      boolean active,
      Instant lastPolledAt,
      Instant lastSuccessfulSyncAt,
      String lastSyncStatus,
      String lastError) {
    this(
        id,
        RiotPlatform.defaultPlatform(),
        gameName,
        tagLine,
        puuid,
        active,
        null,
        lastPolledAt,
        null,
        PlayerBackfillMode.NONE,
        lastSuccessfulSyncAt,
        lastSyncStatus,
        lastError,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public PlayerView(
      Long id,
      RiotPlatform platform,
      String gameName,
      String tagLine,
      String puuid,
      boolean active,
      Instant lastPolledAt,
      Instant lastSuccessfulSyncAt,
      String lastSyncStatus,
      String lastError) {
    this(
        id,
        platform,
        gameName,
        tagLine,
        puuid,
        active,
        null,
        lastPolledAt,
        null,
        PlayerBackfillMode.NONE,
        lastSuccessfulSyncAt,
        lastSyncStatus,
        lastError,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public PlayerView(
      Long id,
      RiotPlatform platform,
      String gameName,
      String tagLine,
      String puuid,
      boolean active,
      Instant archivedAt,
      Instant lastPolledAt,
      Instant lastSuccessfulSyncAt,
      String lastSyncStatus,
      String lastError,
      String rankQueueType,
      String rankTier,
      String rankDivision,
      Integer rankLeaguePoints,
      Integer rankScore,
      Instant rankUpdatedAt) {
    this(
        id,
        platform,
        gameName,
        tagLine,
        puuid,
        active,
        archivedAt,
        lastPolledAt,
        null,
        PlayerBackfillMode.NONE,
        lastSuccessfulSyncAt,
        lastSyncStatus,
        lastError,
        rankQueueType,
        rankTier,
        rankDivision,
        rankLeaguePoints,
        rankScore,
        rankUpdatedAt);
  }

  public static PlayerView fromEntity(PlayerEntity player) {
    return new PlayerView(
        player.getId(),
        RiotPlatform.fromFormValue(player.getPlatform()),
        player.getGameName(),
        player.getTagLine(),
        player.getPuuid(),
        player.isActive(),
        player.getArchivedAt(),
        player.getLastPolledAt(),
        player.getTrackFrom(),
        player.getBackfillMode() == null ? PlayerBackfillMode.NONE : player.getBackfillMode(),
        player.getLastSuccessfulSyncAt(),
        player.getLastSyncStatus(),
        player.getLastError(),
        player.getRankQueueType(),
        player.getRankTier(),
        player.getRankDivision(),
        player.getRankLeaguePoints(),
        player.getRankScore(),
        player.getRankUpdatedAt());
  }

  public boolean archived() {
    return archivedAt != null;
  }

  public String displayName() {
    return gameName + "#" + tagLine;
  }

  public String operationalStatus() {
    if (archived()) {
      return "ARCHIVED";
    }
    if (!active) {
      return "INACTIVE";
    }
    return lastSyncStatus == null || lastSyncStatus.isBlank() ? "NEW" : lastSyncStatus;
  }

  public boolean importsBackfillWithoutNotifications() {
    return backfillMode == PlayerBackfillMode.IMPORT_WITHOUT_NOTIFICATIONS;
  }

  public String rankDisplay() {
    if (rankTier == null || rankTier.isBlank() || rankDivision == null || rankDivision.isBlank()) {
      return "Sin rank";
    }
    int points = rankLeaguePoints == null ? 0 : rankLeaguePoints;
    return formatTier(rankTier) + " " + rankDivision + " " + points + " LP";
  }

  public String rankQueueLabel() {
    if (rankQueueType == null || rankQueueType.isBlank()) {
      return "Sin cola";
    }
    return switch (rankQueueType) {
      case "RANKED_SOLO_5x5" -> "Solo/Duo";
      case "RANKED_FLEX_SR" -> "Flex";
      case "ROSTER_AVERAGE" -> "Media roster";
      default -> rankQueueType;
    };
  }

  private String formatTier(String value) {
    String lower = value.toLowerCase();
    return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
  }
}

