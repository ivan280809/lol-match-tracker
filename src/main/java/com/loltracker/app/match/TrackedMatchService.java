package com.loltracker.app.match;

import com.loltracker.app.player.PlayerEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrackedMatchService {

  private final TrackedMatchRepository trackedMatchRepository;

  @Transactional(readOnly = true)
  public boolean exists(PlayerEntity player, String matchId) {
    return trackedMatchRepository.existsByPlayerIdAndMatchId(player.getId(), matchId);
  }

  @Transactional(readOnly = true)
  public Optional<TrackedMatchEntity> findExisting(PlayerEntity player, String matchId) {
    return trackedMatchRepository.findByPlayerIdAndMatchId(player.getId(), matchId);
  }

  @Transactional(readOnly = true)
  public List<TrackedMatchEntity> getPendingNotifications(PlayerEntity player) {
    return trackedMatchRepository.findAllByPlayerIdAndNotificationSentFalseOrderByGameEndAtAsc(player.getId());
  }

  @Transactional
  public TrackedMatchEntity create(PlayerEntity player, MatchSummary summary) {
    TrackedMatchEntity entity = new TrackedMatchEntity();
    entity.setPlayer(player);
    entity.setMatchId(summary.matchId());
    entity.setChampionName(summary.championName());
    entity.setResult(summary.win() ? "VICTORY" : "DEFEAT");
    entity.setGameMode(summary.gameMode());
    entity.setDurationSeconds(summary.durationSeconds());
    entity.setGameEndAt(summary.gameEndAt());
    entity.setNotificationSent(false);
    return trackedMatchRepository.save(entity);
  }

  @Transactional
  public void markNotificationSent(TrackedMatchEntity entity) {
    entity.setNotificationSent(true);
    entity.setNotificationSentAt(Instant.now());
    trackedMatchRepository.save(entity);
  }

  @Transactional(readOnly = true)
  public List<TrackedMatchView> getRecentMatches() {
    return trackedMatchRepository.findTop20ByOrderByGameEndAtDesc().stream()
        .map(TrackedMatchView::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<TrackedMatchView> getRecentMatchesForPlayer(Long playerId, int limit) {
    return trackedMatchRepository
        .findByPlayerIdOrderByGameEndAtDesc(playerId, PageRequest.of(0, Math.max(1, limit)))
        .stream()
        .map(TrackedMatchView::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public PlayerRecentStatsView getRecentStatsForPlayer(Long playerId) {
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
    return trackedMatchRepository.findAll().stream()
        .filter(match -> from == null || !match.getGameEndAt().isBefore(from))
        .filter(match -> to == null || match.getGameEndAt().isBefore(to))
        .filter(match -> normalizedChampion.isBlank() || contains(match.getChampionName(), normalizedChampion))
        .filter(
            match ->
                normalizedText.isBlank()
                    || contains(match.getChampionName(), normalizedText)
                    || contains(match.getGameMode(), normalizedText)
                    || contains(match.getMatchId(), normalizedText)
                    || contains(match.getResult(), normalizedText)
                    || contains(match.getPlayer().getGameName(), normalizedText)
                    || contains(match.getPlayer().getTagLine(), normalizedText))
        .map(match -> match.getPlayer().getId())
        .collect(Collectors.toSet());
  }

  @Transactional(readOnly = true)
  public long countMatches() {
    return trackedMatchRepository.count();
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
}
