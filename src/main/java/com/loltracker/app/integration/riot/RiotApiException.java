package com.loltracker.app.integration.riot;

import java.time.Duration;

public class RiotApiException extends IllegalArgumentException {

  private final RiotErrorCategory category;
  private final Duration retryAfter;

  public RiotApiException(RiotErrorCategory category, String message) {
    this(category, message, null, null);
  }

  public RiotApiException(RiotErrorCategory category, String message, Throwable cause) {
    this(category, message, cause, null);
  }

  public RiotApiException(
      RiotErrorCategory category, String message, Throwable cause, Duration retryAfter) {
    super(message, cause);
    this.category = category;
    this.retryAfter = retryAfter;
  }

  public RiotErrorCategory category() {
    return category;
  }

  public Duration retryAfter() {
    return retryAfter;
  }
}
