package com.loltracker.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.context.annotation.Primary;

/**
 * Separate {@link TaskScheduler} beans for polling and notification dispatching.
 *
 * The default scheduler used by Spring Boot is a single thread pool. When both
 * the polling and the notification dispatcher share the same executor a slow
 * Telegram call can block the poll cycle. To prevent that, we expose two
 * distinct schedulers and explicitly reference them from the @Scheduled
 * annotations in {@link com.loltracker.app.tracking.PollingService} and
 * {@link com.loltracker.app.notification.NotificationOutboxDispatcherService}.
 */
@Configuration
public class SchedulerConfig {

  /**
   * Scheduler dedicated to the polling cycle. A single thread is sufficient as
   * the poll is intentionally serial.
   */
  @Bean(name = "pollingScheduler")
  public TaskScheduler pollingScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(1);
    scheduler.setThreadNamePrefix("polling-scheduler-");
    return scheduler;
  }

  /**
   * Scheduler dedicated to notification dispatching. It can run independently
   * from the poll and can be scaled separately if needed.
   */
  @Bean(name = "dispatcherScheduler")
  @Primary
  public TaskScheduler dispatcherScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(1);
    scheduler.setThreadNamePrefix("dispatcher-scheduler-");
    return scheduler;
  }
}
