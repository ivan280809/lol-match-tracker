package com.loltracker.app.notification;

public record NotificationDispatchResult(int sent, int failed, boolean rateLimited) {

  public NotificationDispatchResult(int sent, int failed) {
    this(sent, failed, false);
  }

  public static NotificationDispatchResult empty() {
    return new NotificationDispatchResult(0, 0);
  }

  public NotificationDispatchResult plus(NotificationDispatchResult other) {
    return new NotificationDispatchResult(
        sent + other.sent(), failed + other.failed(), rateLimited || other.rateLimited());
  }
}
