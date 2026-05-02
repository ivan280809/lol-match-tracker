package com.loltracker.app.settings;

import java.util.Arrays;

public enum RiotRegion {
  EUROPE("Europe", "https://europe.api.riotgames.com"),
  AMERICAS("Americas", "https://americas.api.riotgames.com"),
  ASIA("Asia", "https://asia.api.riotgames.com"),
  SEA("SEA", "https://sea.api.riotgames.com");

  private final String displayName;
  private final String baseUrl;

  RiotRegion(String displayName, String baseUrl) {
    this.displayName = displayName;
    this.baseUrl = baseUrl;
  }

  public String displayName() {
    return displayName;
  }

  public String baseUrl() {
    return baseUrl;
  }

  public static RiotRegion from(String value) {
    return Arrays.stream(values())
        .filter(region -> region.name().equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported Riot region"));
  }
}
