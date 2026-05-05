package com.loltracker.app.match;

public enum MatchResult {
  VICTORY,
  DEFEAT,
  UNKNOWN;

  public static MatchResult fromWin(boolean win) {
    return win ? VICTORY : DEFEAT;
  }
}
