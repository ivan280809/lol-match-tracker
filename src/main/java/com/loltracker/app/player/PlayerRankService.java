package com.loltracker.app.player;

import com.loltracker.app.integration.riot.RiotRankEntry;
import com.loltracker.app.integration.riot.RiotRankPort;
import java.time.Clock;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Objects;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class PlayerRankService {

  public static final String SOLO_QUEUE = "RANKED_SOLO_5x5";
  public static final String FLEX_QUEUE = "RANKED_FLEX_SR";
  private static final Set<String> SUPPORTED_RANK_QUEUES = Set.of(SOLO_QUEUE, FLEX_QUEUE);

  private final RiotRankPort riotRankPort;
  private final PlayerRepository playerRepository;
  private final PlayerRankRepository playerRankRepository;
  private final Clock clock;

  @Value("${app.rank.cache.ttl:PT15M}")
  private Duration rankCacheTtl = Duration.ofMinutes(15);

  public PlayerRankService(
      com.loltracker.app.integration.riot.RiotClient riotClient, PlayerRepository playerRepository) {
    this(riotClient, playerRepository, null, Clock.systemUTC());
  }

  @Autowired
  public PlayerRankService(
      RiotRankPort riotRankPort,
      PlayerRepository playerRepository,
      PlayerRankRepository playerRankRepository,
      Clock clock) {
    this.riotRankPort = riotRankPort;
    this.playerRepository = playerRepository;
    this.playerRankRepository = playerRankRepository;
    this.clock = clock;
  }

  public Optional<PlayerRankSnapshot> refreshRank(PlayerEntity player, String puuid) {
    return refreshRankAvailability(player, puuid).snapshot();
  }

  public RankRefreshResult refreshRankAvailability(PlayerEntity player, String puuid) {
    return refreshRankAvailability(player, puuid, null);
  }

  public RankRefreshResult refreshRankAvailabilityForQueue(
      PlayerEntity player, String puuid, String preferredQueueType) {
    return refreshRankAvailability(player, puuid, normalizeQueueType(preferredQueueType).orElse(null));
  }

  /** Returns a fresh persisted rank for notification work, refreshing Riot only after its TTL. */
  public RankRefreshResult rankForNotification(
      PlayerEntity player, String puuid, String preferredQueueType) {
    String queueType = normalizeQueueType(preferredQueueType).orElse(null);
    Instant now = now();
    Optional<PlayerRankEntity> stored = storedQueueSnapshot(player, queueType);
    if (playerRankRepository != null && player.getId() != null && queueType != null) {
      if (stored.isPresent() && isFresh(stored.get().getUpdatedAt(), now)) {
        PlayerRankEntity entity = stored.get();
        if (entity.getScore() < 0) {
          return RankRefreshResult.cachedUnranked(entity.getUpdatedAt());
        }
        return RankRefreshResult.stored(snapshot(entity), entity.getUpdatedAt());
      }
    } else if (queueType == null
        && player.getRankUpdatedAt() != null
        && isFresh(player.getRankUpdatedAt(), now)) {
      // If we have a fresh stored rank, return it.  If the player has no stored rank
      // (i.e., rank fields are null), the last successful query was an empty
      // result indicating unranked, so we should return an UNRANKED result
      // with the stored timestamp.
      return storedRank(player)
          .map(snapshot -> RankRefreshResult.stored(snapshot, player.getRankUpdatedAt()))
          .orElseGet(() -> RankRefreshResult.cachedUnranked(player.getRankUpdatedAt()));
    }
    return refreshRankAvailability(player, puuid, queueType, stored);
  }

  private boolean isFresh(Instant updatedAt, Instant now) {
    return updatedAt != null && !updatedAt.plus(rankCacheTtl).isBefore(now);
  }

  private RankRefreshResult refreshRankAvailability(
      PlayerEntity player, String puuid, String preferredQueueType) {
    return refreshRankAvailability(
        player, puuid, preferredQueueType, storedQueueSnapshot(player, preferredQueueType));
  }

  private RankRefreshResult refreshRankAvailability(
      PlayerEntity player,
      String puuid,
      String preferredQueueType,
      Optional<PlayerRankEntity> storedQueueSnapshot) {
    if (puuid == null || puuid.isBlank()) {
      return storedFallback(player, preferredQueueType, storedQueueSnapshot, false);
    }
    try {
      List<PlayerRankSnapshot> snapshots =
          toSnapshots(
              riotRankPort.fetchRankEntries(RiotPlatform.fromFormValue(player.getPlatform()), puuid));
      saveQueueSnapshots(player, snapshots);
      Optional<PlayerRankSnapshot> primarySnapshot = selectBestRank(snapshots);
      saveSnapshot(player, primarySnapshot.orElse(null));
      Optional<PlayerRankSnapshot> selectedSnapshot =
          selectRankForQueue(snapshots, preferredQueueType).or(() -> primarySnapshot);
      if (preferredQueueType != null) {
        selectedSnapshot = selectRankForQueue(snapshots, preferredQueueType);
      }
      return selectedSnapshot
          .map(snapshot -> RankRefreshResult.current(snapshot, now()))
          .orElseGet(() -> RankRefreshResult.unranked(now()));
    } catch (RuntimeException e) {
      log.warn("Rank refresh failed for {}#{}", player.getGameName(), player.getTagLine(), e);
      return storedFallback(player, preferredQueueType, storedQueueSnapshot, true);
    }
  }

  private RankRefreshResult storedFallback(
      PlayerEntity player,
      String preferredQueueType,
      Optional<PlayerRankEntity> storedQueueSnapshot,
      boolean refreshError) {
    if (preferredQueueType != null && storedQueueSnapshot.isPresent()) {
      PlayerRankEntity entity = storedQueueSnapshot.get();
      if (entity.getScore() < 0) {
        // No stored rank information, just signal failure without stored data.
        return refreshError
            ? RankRefreshResult.refreshErrorWithoutStored()
            : RankRefreshResult.noRank();
      }
      // Prefer to return the stored snapshot in case of a refresh error so that callers can
      // still display useful information.  This matches the expectations of the test suite.
      return refreshError
          ? RankRefreshResult.refreshErrorWithStored(snapshot(entity), entity.getUpdatedAt())
          : RankRefreshResult.stored(snapshot(entity), entity.getUpdatedAt());
    }
    if (preferredQueueType != null && !preferredQueueType.equals(player.getRankQueueType())) {
      return refreshError
          ? RankRefreshResult.refreshErrorWithoutStored()
          : RankRefreshResult.noRank();
    }
    return storedRank(player)
        .map(
            snapshot ->
                refreshError
                    ? RankRefreshResult.refreshErrorWithStored(snapshot, player.getRankUpdatedAt())
                    : RankRefreshResult.stored(snapshot, player.getRankUpdatedAt()))
        .orElseGet(
            () -> refreshError
                ? RankRefreshResult.refreshErrorWithoutStored()
                : RankRefreshResult.noRank());
  }

  private Optional<PlayerRankEntity> storedQueueSnapshot(PlayerEntity player, String queueType) {
    if (queueType == null || playerRankRepository == null || player.getId() == null) {
      return Optional.empty();
    }
    return playerRankRepository.findByPlayerIdAndQueueType(player.getId(), queueType);
  }

  @Transactional(readOnly = true)
  public Optional<PlayerRankSnapshot> rosterAverageRank() {
    List<Integer> scores =
        playerRepository.findAllByActiveTrueAndArchivedAtIsNullOrderByGameNameAsc().stream()
            .map(PlayerEntity::getRankScore)
            .filter(score -> score != null && score >= 0)
            .toList();
    if (scores.isEmpty()) {
      return Optional.empty();
    }
    int average = Math.round((float) scores.stream().mapToInt(Integer::intValue).average().orElse(0));
    return Optional.of(fromScore(average));
  }

  public Optional<PlayerRankSnapshot> storedRank(PlayerEntity player) {
    return storedRank(player, null);
  }

  public Optional<PlayerRankSnapshot> storedRank(PlayerEntity player, String preferredQueueType) {
    Optional<String> normalizedQueue = normalizeQueueType(preferredQueueType);
    if (normalizedQueue.isPresent()) {
      Optional<PlayerRankSnapshot> storedSpecific = storedRankSnapshot(player, normalizedQueue.get());
      if (storedSpecific.isPresent()) {
        return storedSpecific;
      }
      if (!normalizedQueue.get().equals(player.getRankQueueType())) {
        return Optional.empty();
      }
    }
    if (player.getRankScore() == null
        || player.getRankTier() == null
        || player.getRankDivision() == null
        || player.getRankLeaguePoints() == null) {
      return Optional.empty();
    }
    return Optional.of(
        new PlayerRankSnapshot(
            player.getRankQueueType(),
            player.getRankTier(),
            player.getRankDivision(),
            player.getRankLeaguePoints(),
            player.getRankScore()));
  }

  @Transactional
  void saveSnapshot(PlayerEntity player, PlayerRankSnapshot snapshot) {
    PlayerEntity target =
        player.getId() == null ? player : playerRepository.findById(player.getId()).orElse(player);
    applySnapshot(target, snapshot);
    playerRepository.save(target);
    // Synchronize detached callers as well as the database row so a later
    // lookup in the same notification batch cannot reuse stale primary data.
    if (target != player) {
      applySnapshot(player, snapshot);
      player.setRankUpdatedAt(target.getRankUpdatedAt());
    }
  }

  @Transactional
  void saveQueueSnapshots(PlayerEntity player, List<PlayerRankSnapshot> snapshots) {
    if (playerRankRepository == null || player.getId() == null) {
      return;
    }
    PlayerEntity target =
        player.getId() == null ? player : playerRepository.findById(player.getId()).orElse(player);
    Map<String, PlayerRankSnapshot> snapshotByQueue = new HashMap<>();
    snapshots.forEach(snapshot -> snapshotByQueue.put(snapshot.queueType(), snapshot));
    List<PlayerRankEntity> existing =
        playerRankRepository.findAllByPlayerIdAndQueueTypeIn(player.getId(), SUPPORTED_RANK_QUEUES);
    Map<String, PlayerRankEntity> existingByQueue = new HashMap<>();
    existing.forEach(entity -> existingByQueue.put(entity.getQueueType(), entity));
    for (String queueType : SUPPORTED_RANK_QUEUES) {
      PlayerRankSnapshot snapshot = snapshotByQueue.get(queueType);
      if (snapshot == null) {
        saveUnrankedQueueSnapshot(target, queueType, existingByQueue);
      } else {
        saveQueueSnapshot(target, snapshot, existingByQueue);
      }
    }
  }

  private void saveUnrankedQueueSnapshot(
      PlayerEntity player, String queueType, Map<String, PlayerRankEntity> existingByQueue) {
    PlayerRankEntity entity = existingByQueue.getOrDefault(queueType, new PlayerRankEntity());
    entity.setPlayer(player);
    entity.setQueueType(queueType);
    entity.setTier("UNRANKED");
    entity.setDivision("IV");
    entity.setLeaguePoints(0);
    entity.setScore(-1);
    entity.setUpdatedAt(now());
    playerRankRepository.save(entity);
  }

  private void saveQueueSnapshot(
      PlayerEntity player, PlayerRankSnapshot snapshot, Map<String, PlayerRankEntity> existingByQueue) {
    PlayerRankEntity entity = existingByQueue.getOrDefault(snapshot.queueType(), new PlayerRankEntity());
    entity.setPlayer(player);
    entity.setQueueType(snapshot.queueType());
    entity.setTier(snapshot.tier());
    entity.setDivision(snapshot.division());
    entity.setLeaguePoints(snapshot.leaguePoints());
    entity.setScore(snapshot.score());
    entity.setUpdatedAt(now());
    playerRankRepository.save(entity);
  }

  private List<PlayerRankSnapshot> toSnapshots(List<RiotRankEntry> entries) {
    return entries.stream()
        .filter(entry -> SUPPORTED_RANK_QUEUES.contains(entry.queueType()))
        .map(this::toSnapshot)
        .toList();
  }

  private PlayerRankSnapshot toSnapshot(RiotRankEntry entry) {
    return new PlayerRankSnapshot(
        entry.queueType(),
        entry.tier(),
        normalizeDivision(entry.rank()),
        entry.leaguePoints(),
        score(entry.tier(), entry.rank(), entry.leaguePoints()));
  }

  private Optional<PlayerRankSnapshot> selectBestRank(List<PlayerRankSnapshot> snapshots) {
    return snapshots.stream().min(Comparator.comparingInt(this::queuePriority));
  }

  private Optional<PlayerRankSnapshot> selectRankForQueue(
      List<PlayerRankSnapshot> snapshots, String preferredQueueType) {
    return normalizeQueueType(preferredQueueType)
        .flatMap(
            normalizedQueue ->
                snapshots.stream()
                    .filter(snapshot -> normalizedQueue.equals(snapshot.queueType()))
                    .findFirst());
  }

  private Optional<PlayerRankSnapshot> storedRankSnapshot(PlayerEntity player, String queueType) {
    if (playerRankRepository == null || player.getId() == null) {
      return Optional.empty();
    }
    return playerRankRepository
        .findByPlayerIdAndQueueType(player.getId(), queueType)
        .filter(entity -> entity.getScore() >= 0)
        .map(this::snapshot);
  }

  private PlayerRankSnapshot snapshot(PlayerRankEntity entity) {
    return new PlayerRankSnapshot(
        entity.getQueueType(),
        entity.getTier(),
        entity.getDivision(),
        entity.getLeaguePoints(),
        entity.getScore());
  }

  private int queuePriority(PlayerRankSnapshot snapshot) {
    return SOLO_QUEUE.equals(snapshot.queueType()) ? 0 : 1;
  }

  private void applySnapshot(PlayerEntity player, PlayerRankSnapshot snapshot) {
    if (snapshot == null) {
      player.setRankQueueType(null);
      player.setRankTier(null);
      player.setRankDivision(null);
      player.setRankLeaguePoints(null);
      player.setRankScore(null);
      player.setRankUpdatedAt(now());
      return;
    }
    player.setRankQueueType(snapshot.queueType());
    player.setRankTier(snapshot.tier());
    player.setRankDivision(snapshot.division());
    player.setRankLeaguePoints(snapshot.leaguePoints());
    player.setRankScore(snapshot.score());
    player.setRankUpdatedAt(now());
  }

  private PlayerRankSnapshot fromScore(int score) {
    String tier = tierFromScore(score);
    int tierBase = tierBase(tier);
    int tierProgress = Math.max(0, score - tierBase);
    int divisionStep = Math.min(3, tierProgress / 100);
    int leaguePoints = Math.max(0, Math.min(99, tierProgress % 100));
    return new PlayerRankSnapshot("ROSTER_AVERAGE", tier, divisionFromStep(divisionStep), leaguePoints, score);
  }

  private int score(String tier, String division, int leaguePoints) {
    return tierBase(tier) + divisionBase(division) + Math.max(0, leaguePoints);
  }

  private String tierFromScore(int score) {
    if (score >= 3600) return "CHALLENGER";
    if (score >= 3200) return "GRANDMASTER";
    if (score >= 2800) return "MASTER";
    if (score >= 2400) return "DIAMOND";
    if (score >= 2000) return "EMERALD";
    if (score >= 1600) return "PLATINUM";
    if (score >= 1200) return "GOLD";
    if (score >= 800) return "SILVER";
    if (score >= 400) return "BRONZE";
    return "IRON";
  }

  private int tierBase(String tier) {
    return switch (safeUpper(tier)) {
      case "BRONZE" -> 400;
      case "SILVER" -> 800;
      case "GOLD" -> 1200;
      case "PLATINUM" -> 1600;
      case "EMERALD" -> 2000;
      case "DIAMOND" -> 2400;
      case "MASTER" -> 2800;
      case "GRANDMASTER" -> 3200;
      case "CHALLENGER" -> 3600;
      default -> 0;
    };
  }

  private int divisionBase(String division) {
    return switch (normalizeDivision(division)) {
      case "III" -> 100;
      case "II" -> 200;
      case "I" -> 300;
      default -> 0;
    };
  }

  private String divisionFromStep(int step) {
    return switch (step) {
      case 3 -> "I";
      case 2 -> "II";
      case 1 -> "III";
      default -> "IV";
    };
  }

  private String normalizeDivision(String division) {
    String normalized = safeUpper(division);
    return normalized.isBlank() ? "IV" : normalized;
  }

  private String safeUpper(String value) {
    return value == null ? "" : value.trim().toUpperCase();
  }

  private Optional<String> normalizeQueueType(String queueType) {
    if (queueType == null || queueType.isBlank()) {
      return Optional.empty();
    }
    String candidate = queueType.trim();
    return SUPPORTED_RANK_QUEUES.stream()
        .filter(supported -> supported.equalsIgnoreCase(candidate))
        .findFirst();
  }

  private java.time.Instant now() {
    return clock == null ? java.time.Instant.now() : clock.instant();
  }

  public record RankRefreshResult(
      Optional<PlayerRankSnapshot> snapshot,
      RankAvailability availability,
      Instant updatedAt,
      boolean refreshed) {

    public RankRefreshResult(Optional<PlayerRankSnapshot> snapshot, RankAvailability availability) {
      this(snapshot, availability, null, false);
    }

    public static RankRefreshResult current(PlayerRankSnapshot snapshot) {
      return new RankRefreshResult(Optional.of(snapshot), RankAvailability.CURRENT, null, true);
    }

    public static RankRefreshResult current(PlayerRankSnapshot snapshot, Instant updatedAt) {
      return new RankRefreshResult(Optional.of(snapshot), RankAvailability.CURRENT, updatedAt, true);
    }

    public static RankRefreshResult stored(PlayerRankSnapshot snapshot) {
      return new RankRefreshResult(Optional.of(snapshot), RankAvailability.STORED, null, false);
    }

    public static RankRefreshResult stored(PlayerRankSnapshot snapshot, Instant updatedAt) {
      return new RankRefreshResult(Optional.of(snapshot), RankAvailability.STORED, updatedAt, false);
    }

    public static RankRefreshResult unranked() {
      return new RankRefreshResult(Optional.empty(), RankAvailability.UNRANKED, null, false);
    }

    public static RankRefreshResult unranked(Instant updatedAt) {
      return new RankRefreshResult(Optional.empty(), RankAvailability.UNRANKED, updatedAt, true);
    }

    public static RankRefreshResult cachedUnranked(Instant updatedAt) {
      return new RankRefreshResult(Optional.empty(), RankAvailability.UNRANKED, updatedAt, false);
    }

    public static RankRefreshResult noRank() {
      return new RankRefreshResult(Optional.empty(), RankAvailability.NO_RANK, null, false);
    }

    public static RankRefreshResult refreshErrorWithStored(PlayerRankSnapshot snapshot) {
      return new RankRefreshResult(Optional.of(snapshot), RankAvailability.REFRESH_ERROR_STORED, null, false);
    }

    public static RankRefreshResult refreshErrorWithStored(PlayerRankSnapshot snapshot, Instant updatedAt) {
      return new RankRefreshResult(Optional.of(snapshot), RankAvailability.REFRESH_ERROR_STORED, updatedAt, false);
    }

    public static RankRefreshResult refreshErrorWithoutStored() {
      return new RankRefreshResult(Optional.empty(), RankAvailability.REFRESH_ERROR_NO_STORED, null, false);
    }
  }

  public enum RankAvailability {
    CURRENT,
    STORED,
    UNRANKED,
    NO_RANK,
    REFRESH_ERROR_STORED,
    REFRESH_ERROR_NO_STORED
  }
}
