package com.loltracker.app.ops;

import com.loltracker.app.player.PlayerEntity;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PollRunService {

  private final PollRunRepository pollRunRepository;
  private final Clock clock;

  @Transactional
  public PollRunEntity startRun() {
    return startRun(null);
  }

  @Transactional
  public PollRunEntity startRun(String owner) {
    PollRunEntity entity = new PollRunEntity();
    entity.setStartedAt(now());
    entity.setStatus("RUNNING");
    entity.setOwner(owner);
    entity.setCurrentStage("Inicializando");
    return pollRunRepository.save(entity);
  }

  @Transactional
  public void updateProgress(PollRunEntity entity, PlayerEntity player, String stage) {
    PollRunEntity managed = pollRunRepository.findById(entity.getId()).orElse(entity);
    managed.setCurrentPlayer(player == null ? null : player.getGameName() + "#" + player.getTagLine());
    managed.setCurrentStage(shortText(stage, 120));
    pollRunRepository.save(managed);
  }

  @Transactional
  public PollRunEntity completeRun(
      PollRunEntity entity,
      int playersProcessed,
      int newMatchesFound,
      int notificationsSent,
      List<String> playerErrors) {
    entity.setFinishedAt(now());
    entity.setStatus(playerErrors.isEmpty() ? "SUCCESS" : "PARTIAL_SUCCESS");
    entity.setPlayersProcessed(playersProcessed);
    entity.setNewMatchesFound(newMatchesFound);
    entity.setNotificationsSent(notificationsSent);
    entity.setErrorSummary(playerErrors.isEmpty() ? null : buildPlayerErrorSummary(playerErrors));
    entity.setCurrentStage(playerErrors.isEmpty() ? "Finalizado" : "Finalizado con errores");
    entity.setCurrentPlayer(null);
    return pollRunRepository.save(entity);
  }

  @Transactional
  public PollRunEntity failRun(
      PollRunEntity entity,
      int playersProcessed,
      int newMatchesFound,
      int notificationsSent,
      String errorSummary) {
    entity.setFinishedAt(now());
    entity.setStatus("ERROR");
    entity.setPlayersProcessed(playersProcessed);
    entity.setNewMatchesFound(newMatchesFound);
    entity.setNotificationsSent(notificationsSent);
    entity.setErrorSummary(
        errorSummary == null ? "Unknown error" : errorSummary.substring(0, Math.min(500, errorSummary.length())));
    entity.setCurrentStage("Error");
    entity.setCurrentPlayer(null);
    return pollRunRepository.save(entity);
  }

  @Transactional
  public PollRunEntity rateLimited(
      PollRunEntity entity,
      int playersProcessed,
      int newMatchesFound,
      int notificationsSent,
      Instant pausedUntil,
      String message) {
    entity.setFinishedAt(now());
    entity.setStatus("RATE_LIMITED");
    entity.setPlayersProcessed(playersProcessed);
    entity.setNewMatchesFound(newMatchesFound);
    entity.setNotificationsSent(notificationsSent);
    entity.setRateLimitPausedUntil(pausedUntil);
    entity.setRateLimitMessage(shortText(message, 500));
    entity.setErrorSummary(shortText(message, 500));
    entity.setCurrentStage("Pausado por rate limit Riot");
    return pollRunRepository.save(entity);
  }

  @Transactional(readOnly = true)
  public List<PollRunView> getRecentRuns() {
    return pollRunRepository.findTop10ByOrderByStartedAtDesc().stream()
        .map(PollRunView::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<PollRunView> getRecentRateLimitRuns() {
    return pollRunRepository.findTop5ByRateLimitPausedUntilIsNotNullOrderByStartedAtDesc().stream()
        .map(PollRunView::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public Optional<PollRunView> getActiveRun() {
    return pollRunRepository.findFirstByStatusOrderByStartedAtDesc("RUNNING").map(PollRunView::fromEntity);
  }

  @Transactional(readOnly = true)
  public Optional<PollRunView> getActiveRateLimitPause() {
    return pollRunRepository
        .findFirstByRateLimitPausedUntilAfterOrderByRateLimitPausedUntilDesc(now())
        .map(PollRunView::fromEntity);
  }

  @Transactional(readOnly = true)
  public boolean isRateLimitPaused() {
    return getActiveRateLimitPause().isPresent();
  }

  @Transactional(readOnly = true)
  public boolean isScheduledRunDue(Duration fixedDelay) {
    Optional<PollRunEntity> lastFinished =
        pollRunRepository.findFirstByFinishedAtIsNotNullOrderByFinishedAtDesc();
    if (lastFinished.isEmpty()) {
      return true;
    }
    return !lastFinished.get().getFinishedAt().plus(fixedDelay).isAfter(now());
  }

  private String buildPlayerErrorSummary(List<String> playerErrors) {
    String prefix =
        playerErrors.size() == 1
            ? "1 player failed: "
            : playerErrors.size() + " players failed: ";
    String joined = String.join("; ", playerErrors);
    String summary = prefix + joined;
    return summary.substring(0, Math.min(500, summary.length()));
  }

  private String shortText(String value, int maxLength) {
    if (value == null) {
      return null;
    }
    return value.substring(0, Math.min(maxLength, value.length()));
  }

  private Instant now() {
    return clock == null ? Instant.now() : clock.instant();
  }
}
