package com.loltracker.app.integration.riot;

public class RiotApiException extends IllegalArgumentException {

  private final RiotErrorCategory category;

  public RiotApiException(RiotErrorCategory category, String message) {
    super(message);
    this.category = category;
  }

  public RiotApiException(RiotErrorCategory category, String message, Throwable cause) {
    super(message, cause);
    this.category = category;
  }

  public RiotErrorCategory category() {
    return category;
  }
}
