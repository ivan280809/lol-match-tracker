package com.loltracker.app.notification;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutboxEntity, Long> {

  Optional<NotificationOutboxEntity> findByTrackedMatchId(Long trackedMatchId);

  Optional<NotificationOutboxEntity>
      findTopByStatusAndLastErrorStartingWithOrderByNextAttemptAtDesc(
          NotificationDeliveryStatus status, String lastErrorPrefix);

  @EntityGraph(attributePaths = {"trackedMatch", "trackedMatch.player"})
  Optional<NotificationOutboxEntity> findFirstByStatusInOrderByCreatedAtAsc(
      Collection<NotificationDeliveryStatus> statuses);

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

  /**
   * Bulk update the nextAttemptAt for all pending notifications that are eligible for retry.
   * This is used to enforce a global cooldown after a Telegram rate limit.
   */
  @Modifying
  @Transactional
  @Query(
      "UPDATE NotificationOutboxEntity e SET e.nextAttemptAt = "
          + "CASE WHEN e.nextAttemptAt < :newTime THEN :newTime ELSE e.nextAttemptAt END "
          + "WHERE e.status IN :statuses")
  int bulkSetNextAttemptAt(
      @Param("statuses") Collection<NotificationDeliveryStatus> statuses,
      @Param("newTime") Instant newTime);
}
