package com.loltracker.app.match;

import com.loltracker.app.player.PlayerEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
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
  public long countMatches() {
    return trackedMatchRepository.count();
  }
}
