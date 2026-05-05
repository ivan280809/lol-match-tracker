package com.loltracker.app.ops;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalCallLogRepository extends JpaRepository<ExternalCallLogEntity, Long> {

  Optional<ExternalCallLogEntity> findFirstByIntegrationAndStatusOrderByOccurredAtDesc(
      String integration, String status);

  List<ExternalCallLogEntity> findTop20ByOrderByOccurredAtDesc();
}
