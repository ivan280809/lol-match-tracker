package com.loltracker.app.notification;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutboxEntity, Long> {

  Optional<NotificationOutboxEntity> findByTrackedMatchId(Long trackedMatchId);

  @EntityGraph(attributePaths = {"trackedMatch", "trackedMatch.player"})
  List<NotificationOutboxEntity>
      findTop50ByTrackedMatchPlayerIdAndStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
          Long playerId, Collection<NotificationDeliveryStatus> statuses, Instant now);

  long countByStatus(NotificationDeliveryStatus status);

  long countByStatusIn(Collection<NotificationDeliveryStatus> statuses);

  @EntityGraph(attributePaths = {"trackedMatch", "trackedMatch.player"})
  List<NotificationOutboxEntity> findTop20ByTrackedMatchPlayerIdAndStatusInOrderByCreatedAtDesc(
      Long playerId, Collection<NotificationDeliveryStatus> statuses);

  @EntityGraph(attributePaths = {"trackedMatch", "trackedMatch.player"})
  List<NotificationOutboxEntity> findTop20ByStatusInOrderByCreatedAtDesc(
      Collection<NotificationDeliveryStatus> statuses);
}
