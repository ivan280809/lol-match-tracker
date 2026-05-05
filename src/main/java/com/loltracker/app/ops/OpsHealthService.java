package com.loltracker.app.ops;

import com.loltracker.app.notification.NotificationService;
import com.loltracker.app.settings.AppConfigurationService;
import com.loltracker.app.settings.AppConfigurationView;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpsHealthService {

  private final JdbcTemplate jdbcTemplate;
  private final AppConfigurationService appConfigurationService;
  private final NotificationService notificationService;
  private final ExternalCallLogService externalCallLogService;

  public IntegrationHealthView currentHealth() {
    DatabaseCheck databaseCheck = checkDatabase();
    AppConfigurationView configuration = appConfigurationService.getView();
    return new IntegrationHealthView(
        databaseCheck.ok(),
        databaseCheck.summary(),
        configuration.riotApiKeyConfigured(),
        configuration.telegramConfigured(),
        externalCallLogService.lastOkAt("RIOT").orElse(null),
        externalCallLogService.lastOkAt("TELEGRAM").orElse(null),
        notificationService.countPendingNotifications(),
        notificationService.countFailedNotifications());
  }

  private DatabaseCheck checkDatabase() {
    try {
      Integer value = jdbcTemplate.queryForObject("select 1", Integer.class);
      return new DatabaseCheck(Integer.valueOf(1).equals(value), "DB OK");
    } catch (RuntimeException e) {
      String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
      return new DatabaseCheck(false, message.substring(0, Math.min(120, message.length())));
    }
  }

  private record DatabaseCheck(boolean ok, String summary) {}
}
