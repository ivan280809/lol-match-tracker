package com.loltracker.app.ops;

import com.loltracker.app.match.TrackedMatchService;
import com.loltracker.app.player.PlayerService;
import com.loltracker.app.player.PlayerView;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RosterQueryService {

  private final PlayerService playerService;
  private final TrackedMatchService trackedMatchService;

  public List<PlayerView> getRoster(DashboardFilter filter) {
    Set<Long> historyMatches =
        trackedMatchService.findPlayerIdsMatchingHistory(
            filter.query(), filter.champion(), filter.fromDate(), filter.toDate());
    return playerService.getAllPlayers().stream()
        .filter(player -> matchesStatus(player, filter.status()))
        .filter(player -> !filter.withError() || hasText(player.lastError()))
        .filter(player -> filter.platform() == null || filter.platform() == player.platform())
        .filter(player -> matchesSyncStatus(player, filter.syncStatus()))
        .filter(player -> !filter.hasHistorySpecificFilter() || historyMatches.contains(player.id()))
        .filter(player -> matchesText(player, filter.query(), historyMatches))
        .sorted(comparator(filter.sort()))
        .toList();
  }

  private boolean matchesStatus(PlayerView player, String status) {
    return switch (status) {
      case "all" -> true;
      case "inactive" -> !player.active() && !player.archived();
      case "archived" -> player.archived();
      case "active" -> player.active() && !player.archived();
      default -> player.active() && !player.archived();
    };
  }

  private boolean matchesSyncStatus(PlayerView player, String syncStatus) {
    return !hasText(syncStatus)
        || (player.lastSyncStatus() != null && player.lastSyncStatus().equalsIgnoreCase(syncStatus));
  }

  private boolean matchesText(PlayerView player, String query, Set<Long> historyMatches) {
    if (!hasText(query)) {
      return true;
    }
    String normalized = query.toLowerCase();
    return contains(player.gameName(), normalized)
        || contains(player.tagLine(), normalized)
        || contains(player.puuid(), normalized)
        || contains(player.lastError(), normalized)
        || player.platform().name().toLowerCase().contains(normalized)
        || player.platform().shortName().toLowerCase().contains(normalized)
        || historyMatches.contains(player.id());
  }

  private Comparator<PlayerView> comparator(String sort) {
    Comparator<PlayerView> base =
        switch (sort) {
          case "last-error" -> this::compareLastError;
          case "last-sync" -> (left, right) -> compareInstantDesc(left.lastSuccessfulSyncAt(), right.lastSuccessfulSyncAt());
          case "rank" -> this::compareRank;
          case "activity" -> (left, right) -> compareInstantDesc(left.lastPolledAt(), right.lastPolledAt());
          default -> Comparator.comparing(PlayerView::gameName, String.CASE_INSENSITIVE_ORDER)
              .thenComparing(PlayerView::tagLine, String.CASE_INSENSITIVE_ORDER);
        };
    return base.thenComparing(PlayerView::gameName, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(PlayerView::tagLine, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(PlayerView::id);
  }

  private int compareLastError(PlayerView left, PlayerView right) {
    int errorCompare = Boolean.compare(hasText(right.lastError()), hasText(left.lastError()));
    if (errorCompare != 0) {
      return errorCompare;
    }
    return compareInstantDesc(left.lastPolledAt(), right.lastPolledAt());
  }

  private int compareRank(PlayerView left, PlayerView right) {
    Integer leftScore = left.rankScore();
    Integer rightScore = right.rankScore();
    if (leftScore == null && rightScore == null) {
      return 0;
    }
    if (leftScore == null) {
      return 1;
    }
    if (rightScore == null) {
      return -1;
    }
    return Integer.compare(rightScore, leftScore);
  }

  private int compareInstantDesc(Instant left, Instant right) {
    if (left == null && right == null) {
      return 0;
    }
    if (left == null) {
      return 1;
    }
    if (right == null) {
      return -1;
    }
    return right.compareTo(left);
  }

  private boolean contains(String value, String normalizedQuery) {
    return value != null && value.toLowerCase().contains(normalizedQuery);
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
