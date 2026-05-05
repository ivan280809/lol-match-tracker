package com.loltracker.app.ops;

import com.loltracker.app.match.TrackedMatchService;
import com.loltracker.app.notification.NotificationService;
import com.loltracker.app.player.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerPageService {

  private final PlayerService playerService;
  private final TrackedMatchService trackedMatchService;
  private final NotificationService notificationService;

  public PlayerDetailView getDetail(Long playerId) {
    return new PlayerDetailView(
        playerService.getPlayer(playerId),
        trackedMatchService.getRecentMatchesForPlayer(playerId, 20),
        trackedMatchService.getRecentStatsForPlayer(playerId),
        notificationService.getProblemNotificationsForPlayer(playerId));
  }
}
