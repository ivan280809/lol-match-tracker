package com.loltracker.app.ops;

import com.loltracker.app.notification.NotificationService;
import com.loltracker.app.player.PlayerService;
import com.loltracker.app.player.PlayerView;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

  private final PlayerService playerService;
  private final NotificationService notificationService;
  private final PollRunService pollRunService;
  private final ExternalCallLogService externalCallLogService;

  public AuditView getAudit() {
    return new AuditView(
        playerService.getAllPlayers().stream()
            .filter(player -> player.lastError() != null && !player.lastError().isBlank())
            .sorted(
                Comparator.comparing(
                        PlayerView::lastPolledAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(PlayerView::gameName, String.CASE_INSENSITIVE_ORDER))
            .limit(20)
            .toList(),
        notificationService.getRecentProblemNotifications(),
        pollRunService.getRecentRuns(),
        pollRunService.getRecentRateLimitRuns(),
        externalCallLogService.recentLogs());
  }
}
