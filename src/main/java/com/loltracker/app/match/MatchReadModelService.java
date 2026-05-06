package com.loltracker.app.match;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchReadModelService {

  private final MatchRepository matchRepository;
  private final PlayerMatchRepository playerMatchRepository;

  @Transactional(readOnly = true)
  public boolean exists(Long playerId, String matchId) {
    return playerMatchRepository.existsByPlayerIdAndMatchMatchId(playerId, matchId);
  }

  @Transactional(readOnly = true)
  public List<TrackedMatchView> getRecentMatches(int limit) {
    return playerMatchRepository.findRecent(page(limit)).stream().map(TrackedMatchView::fromPlayerMatch).toList();
  }

  @Transactional(readOnly = true)
  public List<TrackedMatchView> getRecentMatchesForPlayer(Long playerId, int limit) {
    return playerMatchRepository.findRecentForPlayer(playerId, page(limit)).stream()
        .map(TrackedMatchView::fromPlayerMatch)
        .toList();
  }

  @Transactional(readOnly = true)
  public PlayerRecentStatsView getRecentStatsForPlayer(Long playerId) {
    List<PlayerMatchEntity> matches = playerMatchRepository.findRecentForPlayer(playerId, PageRequest.of(0, 30));
    if (matches.isEmpty()) {
      return PlayerRecentStatsView.empty();
    }
    int wins = wins(matches);
    int losses = matches.size() - wins;
    long averageDuration =
        Math.round(matches.stream().mapToLong(match -> match.getMatch().getDurationSeconds()).average().orElse(0));
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
    Instant to = toDate == null ? null : fromDateExclusive(toDate);
    return playerMatchRepository.findAllForHistorySearch().stream()
        .filter(match -> from == null || !match.getMatch().getGameEndAt().isBefore(from))
        .filter(match -> to == null || match.getMatch().getGameEndAt().isBefore(to))
        .filter(match -> normalizedChampion.isBlank() || contains(match.getChampionName(), normalizedChampion))
        .filter(match -> matchesText(match, normalizedText))
        .map(match -> match.getPlayer().getId())
        .collect(Collectors.toSet());
  }

  @Transactional(readOnly = true)
  public long countMatches() {
    return matchRepository.count();
  }

  @Transactional(readOnly = true)
  public List<SharedMatchStatsView> getSharedMatchStats(int limit) {
    List<Long> matchIds = playerMatchRepository.findSharedMatchIds(page(limit));
    if (matchIds.isEmpty()) {
      return List.of();
    }
    Map<Long, Integer> order = order(matchIds);
    return playerMatchRepository.findByMatchIdsWithPlayerAndMatch(matchIds).stream()
        .collect(Collectors.groupingBy(match -> match.getMatch().getId(), LinkedHashMap::new, Collectors.toList()))
        .entrySet()
        .stream()
        .sorted(Comparator.comparingInt(entry -> order.getOrDefault(entry.getKey(), Integer.MAX_VALUE)))
        .map(entry -> sharedStats(entry.getValue()))
        .toList();
  }

  @Transactional(readOnly = true)
  public Optional<SharedMatchStatsView> getSharedMatchStats(String matchId) {
    List<PlayerMatchEntity> matches = playerMatchRepository.findByMatchMatchIdWithPlayerAndMatch(matchId);
    if (matches.size() < 2) {
      return Optional.empty();
    }
    return Optional.of(sharedStats(matches));
  }

  private SharedMatchStatsView sharedStats(List<PlayerMatchEntity> playerMatches) {
    PlayerMatchEntity first = playerMatches.get(0);
    MatchEntity match = first.getMatch();
    MatchQueueDescriptor queue = MatchQueueCatalog.describe(match.getQueueId(), match.getGameMode());
    int wins = wins(playerMatches);
    return new SharedMatchStatsView(
        match.getMatchId(),
        queue.queueId(),
        queue.label(),
        queue.type(),
        match.getGameEndAt(),
        playerMatches.size(),
        wins,
        playerMatches.size() - wins,
        roundOneDecimal(playerMatches.stream().mapToInt(PlayerMatchEntity::getKills).average().orElse(0)),
        roundOneDecimal(playerMatches.stream().mapToInt(PlayerMatchEntity::getDeaths).average().orElse(0)),
        roundOneDecimal(playerMatches.stream().mapToInt(PlayerMatchEntity::getAssists).average().orElse(0)));
  }

  private Map<Long, Integer> order(List<Long> matchIds) {
    return java.util.stream.IntStream.range(0, matchIds.size())
        .boxed()
        .collect(Collectors.toMap(matchIds::get, Function.identity()));
  }

  private String recentForm(List<PlayerMatchEntity> matches) {
    return matches.stream().limit(10).map(match -> isVictory(match) ? "W" : "L").collect(Collectors.joining(" "));
  }

  private List<ChampionUsageView> favoriteChampions(List<PlayerMatchEntity> matches) {
    Map<String, List<PlayerMatchEntity>> byChampion =
        matches.stream()
            .collect(
                Collectors.groupingBy(
                    PlayerMatchEntity::getChampionName,
                    LinkedHashMap::new,
                    Collectors.toList()));
    return byChampion.entrySet().stream()
        .map(
            entry -> {
              int wins = wins(entry.getValue());
              int games = entry.getValue().size();
              return new ChampionUsageView(entry.getKey(), games, wins, games - wins, winRate(wins, games));
            })
        .sorted(
            Comparator.comparingInt(ChampionUsageView::games)
                .reversed()
                .thenComparing(ChampionUsageView::championName))
        .limit(5)
        .toList();
  }

  private int wins(List<PlayerMatchEntity> matches) {
    return (int) matches.stream().filter(this::isVictory).count();
  }

  private boolean isVictory(PlayerMatchEntity match) {
    return match.getResult() == MatchResult.VICTORY;
  }

  private boolean matchesText(PlayerMatchEntity match, String normalizedText) {
    if (normalizedText.isBlank()) {
      return true;
    }
    MatchEntity globalMatch = match.getMatch();
    MatchQueueDescriptor queue = MatchQueueCatalog.describe(globalMatch.getQueueId(), globalMatch.getGameMode());
    return contains(match.getChampionName(), normalizedText)
        || contains(globalMatch.getGameMode(), normalizedText)
        || contains(queue.label(), normalizedText)
        || contains(globalMatch.getMatchId(), normalizedText)
        || contains(match.getResult().name(), normalizedText)
        || contains(match.getPlayer().getGameName(), normalizedText)
        || contains(match.getPlayer().getTagLine(), normalizedText);
  }

  private int winRate(int wins, int total) {
    if (total <= 0) {
      return 0;
    }
    return Math.round((wins * 100f) / total);
  }

  private PageRequest page(int limit) {
    return PageRequest.of(0, Math.max(1, limit));
  }

  private Instant fromDateExclusive(LocalDate toDate) {
    return toDate == null ? null : toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
  }

  private double roundOneDecimal(double value) {
    return Math.round(value * 10.0) / 10.0;
  }

  private boolean contains(String value, String query) {
    return value != null && value.toLowerCase().contains(query);
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase();
  }
}
