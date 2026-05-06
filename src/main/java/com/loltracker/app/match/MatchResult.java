package com.loltracker.app.match;

public enum MatchResult {
  VICTORY,
  DEFEAT,
  UNKNOWN;

  public static MatchResult fromWin(boolean win) {
    return win ? VICTORY : DEFEAT;
  }

  public static MatchResult fromStoredValue(String value) {
    if (value == null || value.isBlank()) {
      return UNKNOWN;
    }
    try {
      return MatchResult.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return UNKNOWN;
    }
  }
}
