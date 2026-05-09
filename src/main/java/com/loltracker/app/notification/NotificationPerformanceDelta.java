package com.loltracker.app.notification;

public record NotificationPerformanceDelta(
    boolean available,
    double kdaRatioDelta,
    double csPerMinuteDelta,
    double goldPerMinuteDelta,
    double damagePerMinuteDelta,
    double visionPerMinuteDelta) {

  public static NotificationPerformanceDelta empty() {
    return new NotificationPerformanceDelta(false, 0, 0, 0, 0, 0);
  }
}
