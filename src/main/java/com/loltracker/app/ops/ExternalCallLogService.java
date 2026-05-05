package com.loltracker.app.ops;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExternalCallLogService {

  private final ExternalCallLogRepository externalCallLogRepository;

  @Transactional
  public ExternalCallLogView recordOk(String integration, String operation, String summary) {
    return record(integration, operation, "OK", "OK", summary);
  }

  @Transactional
  public ExternalCallLogView recordError(
      String integration, String operation, String category, String summary) {
    return record(integration, operation, "ERROR", category, summary);
  }

  @Transactional(readOnly = true)
  public Optional<Instant> lastOkAt(String integration) {
    return externalCallLogRepository
        .findFirstByIntegrationAndStatusOrderByOccurredAtDesc(integration, "OK")
        .map(ExternalCallLogEntity::getOccurredAt);
  }

  @Transactional(readOnly = true)
  public List<ExternalCallLogView> recentLogs() {
    return externalCallLogRepository.findTop20ByOrderByOccurredAtDesc().stream()
        .map(ExternalCallLogView::fromEntity)
        .toList();
  }

  private ExternalCallLogView record(
      String integration, String operation, String status, String category, String summary) {
    ExternalCallLogEntity entity = new ExternalCallLogEntity();
    entity.setIntegration(limit(integration, 24));
    entity.setOperation(limit(operation, 64));
    entity.setStatus(limit(status, 16));
    entity.setCategory(limit(category, 32));
    entity.setSummary(limit(summary, 500));
    return ExternalCallLogView.fromEntity(externalCallLogRepository.save(entity));
  }

  private String limit(String value, int maxLength) {
    if (value == null) {
      return null;
    }
    return value.substring(0, Math.min(maxLength, value.length()));
  }
}
