package com.loltracker.app.match;

import com.loltracker.app.player.PlayerEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TrackedMatchService {

  private final TrackedMatchRepository trackedMatchRepository;
  private final PlayerMatchService playerMatchService;
  private final MatchReadModelService matchReadModelService;
  private final PlayerMatchRepository playerMatchRepository;
  private final Clock clock;

  @Transactional(readOnly = true)
  public boolean exists(PlayerEntity player, String matchId) {
    return trackedMatchRepository.existsByPlayerIdAndMatchId(player.getId(), matchId)
        || (matchReadModelService != null && matchReadModelService.exists(player.getId(), matchId));
  }

  /** Preserve per-player read-model markers when the legacy row is absent. */
  @Transactional(readOnly = true)
  public Optional<TrackedMatchEntity> findExisting(PlayerEntity player, String matchId) {
    Optional<TrackedMatchEntity> legacy =
        trackedMatchRepository.findByPlayerIdAndMatchId(player.getId(), matchId);
    if (legacy.isPresent()
        || matchReadModelService == null
        || !matchReadModelService.exists(player.getId(), matchId)) {
      return legacy;
    }
    TrackedMatchEntity marker = new TrackedMatchEntity();
    marker.setPlayer(player);
    marker.setMatchId(matchId);
    return Optional.of(marker);
  }

  @Transactional(readOnly = true)
  public List<TrackedMatchEntity> getPendingNotifications(PlayerEntity player) {
    return trackedMatchRepository.findAllByPlayerIdAndNotificationSentFalseOrderByGameEndAtAsc(player.getId());
  }

  @Transactional
  public TrackedMatchEntity create(PlayerEntity player, MatchSummary summary) {
    return create(player, summary, false);
  }

  @Transactional
  public TrackedMatchEntity create(PlayerEntity player, MatchSummary summary, boolean notificationSuppressed) {
    if (playerMatchService != null) {
      playerMatchService.record(player, summary, notificationSuppressed);
    }
    TrackedMatchEntity entity = new TrackedMatchEntity();
    entity.setPlayer(player);
    entity.setMatchId(summary.matchId());
    entity.setChampionId(summary.championId());
    entity.setChampionName(summary.championName());
    entity.setResult(summary.win() ? "VICTORY" : "DEFEAT");
    entity.setGameMode(summary.gameMode());
    entity.setQueueId(summary.queueId());
    entity.setLane(summary.lane());
    entity.setRole(summary.role());
    entity.setKills(summary.kills());
    entity.setDeaths(summary.deaths());
    entity.setAssists(summary.assists());
    entity.setCreepScore(summary.creepScore());
    entity.setGoldEarned(summary.goldEarned());
    entity.setDamageDealtToChampions(summary.damageDealtToChampions());
    entity.setVisionScore(summary.visionScore());
    entity.setDurationSeconds(summary.durationSeconds());
    entity.setGameEndAt(summary.gameEndAt());
    entity.setPlatform(summary.platform());
    entity.setRegion(summary.region());
    entity.setNotificationSent(notificationSuppressed);
    if (notificationSuppressed) {
      entity.setNotificationSentAt(now());
    }
    return trackedMatchRepository.save(entity);
  }

  @Transactional
  public void markNotificationSent(TrackedMatchEntity entity) {
    entity.setNotificationSent(true);
    entity.setNotificationSentAt(now());
    trackedMatchRepository.save(entity);
  }

  @Transactional(readOnly = true)
  public List<TrackedMatchView> getRecentMatches() {
    if (matchReadModelService != null) {
      List<TrackedMatchView> globalMatches = matchReadModelService.getRecentMatches(20);
      if (!globalMatches.isEmpty()) {
        return globalMatches;
      }
    }
    return trackedMatchRepository.findTop20ByOrderByGameEndAtDesc().stream()
        .map(TrackedMatchView::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<TrackedMatchView> getRecentMatchesForPlayer(Long playerId, int limit) {
    if (matchReadModelService != null) {
      List<TrackedMatchView> globalMatches = matchReadModelService.getRecentMatchesForPlayer(playerId, limit);
      if (!globalMatches.isEmpty()) {
        return globalMatches;
      }
    }
    return trackedMatchRepository
        .findByPlayerIdOrderByGameEndAtDesc(playerId, PageRequest.of(0, Math.max(1, limit)))
        .stream()
        .map(TrackedMatchView::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public PlayerRecentStatsView getRecentStatsForPlayer(Long playerId) {
    if (matchReadModelService != null) {
      PlayerRecentStatsView globalStats = matchReadModelService.getRecentStatsForPlayer(playerId);
      if (globalStats.games() > 0) {
        return globalStats;
      }
    }
    List<TrackedMatchEntity> matches =
        trackedMatchRepository.findByPlayerIdOrderByGameEndAtDesc(playerId, PageRequest.of(0, 30));
    if (matches.isEmpty()) {
      return PlayerRecentStatsView.empty();
    }
    int wins = wins(matches);
    int losses = matches.size() - wins;
    long averageDuration =
        Math.round(matches.stream().mapToLong(TrackedMatchEntity::getDurationSeconds).average().orElse(0));
    return new PlayerRecentStatsView(
        recentForm(matches),
        matches.size(),
        wins,
        losses,
        winRate(wins, matches.size()),
        averageDuration,
        favoriteChampions(matches));
  }

  @Transactional(readOnly = true)
  public Set<Long> findPlayerIdsMatchingHistory(
      String text, String champion, LocalDate fromDate, LocalDate toDate) {
    String normalizedText = normalize(text);
    String normalizedChampion = normalize(champion);
    boolean hasHistoryFilter =
        !normalizedText.isBlank() || !normalizedChampion.isBlank() || fromDate != null || toDate != null;
    if (!hasHistoryFilter) {
      return Set.of();
    }

    Instant from = fromDate == null ? null : fromDate.atStartOfDay().toInstant(ZoneOffset.UTC);
    Instant to = toDate == null ? null : toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

    // Preserve original source‑precedence semantics: if the global read
    // model returns any results, it is authoritative. Only when the
    // global store has no rows at all do we fall back to the legacy
    // tracked_matches table.
    if (matchReadModelService != null) {
      Set<Long> globalMatches =
          matchReadModelService.findPlayerIdsMatchingHistory(text, champion, fromDate, toDate);
      // If the global store contains any matches, return the global
      // results – even if they are empty, because that indicates
      // legacy filtering is no longer desired.
      if (!globalMatches.isEmpty() || matchReadModelService.countMatches() > 0) {
        return globalMatches;
      }
    }
    // Global store has no rows, fall back to legacy data.
    return trackedMatchRepository
        .findDistinctPlayerIdsMatchingHistory(
            escapeLike(normalizedChampion.isBlank() ? null : normalizedChampion),
            escapeLike(normalizedText.isBlank() ? null : normalizedText),
            from,
            to,
            MatchQueueCatalog.JAVA_WHITESPACE)
        .stream()
        .collect(Collectors.toSet());
  }

  @Transactional(readOnly = true)
  public long countMatches() {
    if (matchReadModelService != null) {
      long globalMatches = matchReadModelService.countMatches();
      if (globalMatches > 0) {
        return globalMatches;
      }
    }
    return trackedMatchRepository.count();
  }

  @Transactional(readOnly = true)
  public List<SharedMatchStatsView> getSharedMatchStats(int limit) {
    return matchReadModelService == null ? List.of() : matchReadModelService.getSharedMatchStats(limit);
  }

  @Transactional(readOnly = true)
  public Optional<SharedMatchStatsView> getSharedMatchStats(String matchId) {
    return matchReadModelService == null ? Optional.empty() : matchReadModelService.getSharedMatchStats(matchId);
  }

  /** Read one page of existing IDs from both operational stores. */
  @Transactional(readOnly = true)
  public Set<String> findExistingMatchIds(PlayerEntity player, Collection<String> matchIds) {
    if (matchIds == null || matchIds.isEmpty()) {
      return Set.of();
    }
    Set<String> existing =
        trackedMatchRepository.findAllByPlayerIdAndMatchIdIn(player.getId(), matchIds).stream()
            .map(TrackedMatchEntity::getMatchId)
            .collect(Collectors.toSet());
    if (matchReadModelService != null) {
      existing.addAll(matchReadModelService.findExistingMatchIds(player.getId(), matchIds));
    }
    return existing;
  }

  private String recentForm(List<TrackedMatchEntity> matches) {
    return matches.stream().limit(10).map(match -> isVictory(match) ? "W" : "L").collect(Collectors.joining(" "));
  }

  private List<ChampionUsageView> favoriteChampions(List<TrackedMatchEntity> matches) {
    Map<String, List<TrackedMatchEntity>> byChampion =
        matches.stream()
            .collect(
                Collectors.groupingBy(
                    TrackedMatchEntity::getChampionName,
                    LinkedHashMap::new,
                    Collectors.toList()));
    return byChampion.entrySet().stream()
        .map(
            entry -> {
              int wins = wins(entry.getValue());
              int games = entry.getValue().size();
              return new ChampionUsageView(
                  entry.getKey(), games, wins, games - wins, winRate(wins, games));
            })
        .sorted(Comparator.comparingInt(ChampionUsageView::games).reversed().thenComparing(ChampionUsageView::championName))
        .limit(5)
        .toList();
  }

  private int wins(List<TrackedMatchEntity> matches) {
    return (int) matches.stream().filter(this::isVictory).count();
  }

  private int winRate(int wins, int total) {
    if (total <= 0) {
      return 0;
    }
    return Math.round((wins * 100f) / total);
  }

  private boolean isVictory(TrackedMatchEntity match) {
    return "VICTORY".equalsIgnoreCase(match.getResult());
  }

  private boolean contains(String value, String query) {
    return value != null && value.toLowerCase().contains(query);
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase();
  }

  /**
   * Escape any SQL LIKE wildcards so that {@code "%"}, {@code "_"} and
   * {@code "\\"} are treated literally.  The query in {@link
   * PlayerMatchRepository#findDistinctPlayerIdsMatchingHistory} declares an
   * escape character of backslash.  This method escapes both the escape
   * character itself and the wildcard characters.
   */
  private String escapeLike(String value) {
    if (value == null) {
      return null;
    }
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  private Instant now() {
    return clock == null ? Instant.now() : clock.instant();
  }
}
