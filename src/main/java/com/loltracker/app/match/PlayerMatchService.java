package com.loltracker.app.match;

import com.loltracker.app.player.PlayerEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerMatchService {

  private final MatchRepository matchRepository;
  private final PlayerMatchRepository playerMatchRepository;

  @Transactional(readOnly = true)
  public boolean exists(PlayerEntity player, String matchId) {
    return playerMatchRepository.existsByPlayerIdAndMatchMatchId(player.getId(), matchId);
  }

  @Transactional
  public PlayerMatchEntity record(PlayerEntity player, MatchSummary summary, boolean notificationSuppressed) {
    return playerMatchRepository
        .findByPlayerIdAndMatchMatchId(player.getId(), summary.matchId())
        .orElseGet(() -> create(player, summary, notificationSuppressed));
  }

  private PlayerMatchEntity create(PlayerEntity player, MatchSummary summary, boolean notificationSuppressed) {
    MatchEntity match = findOrCreateMatch(summary);
    PlayerMatchEntity entity = new PlayerMatchEntity();
    entity.setPlayer(player);
    entity.setMatch(match);
    entity.setPuuid(player.getPuuid() == null ? "" : player.getPuuid());
    entity.setChampionId(summary.championId());
    entity.setChampionName(safe(summary.championName(), "Unknown"));
    entity.setResult(MatchResult.fromWin(summary.win()));
    entity.setLane(safe(summary.lane(), ""));
    entity.setRole(safe(summary.role(), ""));
    entity.setKills(summary.kills());
    entity.setDeaths(summary.deaths());
    entity.setAssists(summary.assists());
    entity.setCreepScore(summary.creepScore());
    entity.setGoldEarned(summary.goldEarned());
    entity.setDamageDealtToChampions(summary.damageDealtToChampions());
    entity.setVisionScore(summary.visionScore());
    entity.setNotificationSuppressed(notificationSuppressed);
    try {
      return playerMatchRepository.save(entity);
    } catch (DataIntegrityViolationException e) {
      return playerMatchRepository.findByPlayerIdAndMatchMatchId(player.getId(), summary.matchId()).orElseThrow(() -> e);
    }
  }

  private MatchEntity findOrCreateMatch(MatchSummary summary) {
    return matchRepository.findByMatchId(summary.matchId()).orElseGet(() -> saveMatch(summary));
  }

  private MatchEntity saveMatch(MatchSummary summary) {
    MatchEntity entity = new MatchEntity();
    entity.setMatchId(summary.matchId());
    entity.setGameMode(safe(summary.gameMode(), "Unknown"));
    entity.setQueueId(summary.queueId());
    entity.setDurationSeconds(summary.durationSeconds());
    entity.setGameEndAt(summary.gameEndAt());
    entity.setPlatform(safe(summary.platform(), ""));
    entity.setRegion(safe(summary.region(), ""));
    try {
      return matchRepository.save(entity);
    } catch (DataIntegrityViolationException e) {
      return matchRepository.findByMatchId(summary.matchId()).orElseThrow(() -> e);
    }
  }

  private String safe(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
