package com.loltracker.app.ops;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PollRunService {

  private final PollRunRepository pollRunRepository;

  @Transactional
  public PollRunEntity startRun() {
    PollRunEntity entity = new PollRunEntity();
    entity.setStartedAt(Instant.now());
    entity.setStatus("RUNNING");
    return pollRunRepository.save(entity);
  }

  @Transactional
  public PollRunEntity completeRun(
      PollRunEntity entity,
      int playersProcessed,
      int newMatchesFound,
      int notificationsSent,
      List<String> playerErrors) {
    entity.setFinishedAt(Instant.now());
    entity.setStatus(playerErrors.isEmpty() ? "SUCCESS" : "PARTIAL_SUCCESS");
    entity.setPlayersProcessed(playersProcessed);
    entity.setNewMatchesFound(newMatchesFound);
    entity.setNotificationsSent(notificationsSent);
    entity.setErrorSummary(playerErrors.isEmpty() ? null : buildPlayerErrorSummary(playerErrors));
    return pollRunRepository.save(entity);
  }

  @Transactional
  public PollRunEntity failRun(
      PollRunEntity entity,
      int playersProcessed,
      int newMatchesFound,
      int notificationsSent,
      String errorSummary) {
    entity.setFinishedAt(Instant.now());
    entity.setStatus("ERROR");
    entity.setPlayersProcessed(playersProcessed);
    entity.setNewMatchesFound(newMatchesFound);
    entity.setNotificationsSent(notificationsSent);
    entity.setErrorSummary(
        errorSummary == null ? "Unknown error" : errorSummary.substring(0, Math.min(500, errorSummary.length())));
    return pollRunRepository.save(entity);
  }

  @Transactional(readOnly = true)
  public List<PollRunView> getRecentRuns() {
    return pollRunRepository.findTop10ByOrderByStartedAtDesc().stream()
        .map(PollRunView::fromEntity)
        .toList();
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
}
