package com.loltracker.app.player;

import java.util.Arrays;

public enum RiotPlatform {
  BR1("BR", "Brazil"),
  EUN1("EUNE", "Europe Nordic & East"),
  EUW1("EUW", "Europe West"),
  JP1("JP", "Japan"),
  KR("KR", "Korea"),
  LA1("LAN", "Latin America North"),
  LA2("LAS", "Latin America South"),
  NA1("NA", "North America"),
  OC1("OCE", "Oceania"),
  TR1("TR", "Turkey"),
  RU("RU", "Russia"),
  PH2("PH", "Philippines"),
  SG2("SG", "Singapore"),
  TH2("TH", "Thailand"),
  TW2("TW", "Taiwan"),
  VN2("VN", "Vietnam");

  private final String shortName;
  private final String displayName;

  RiotPlatform(String shortName, String displayName) {
    this.shortName = shortName;
    this.displayName = displayName;
  }

  public String shortName() {
    return shortName;
  }

  public String displayName() {
    return displayName;
  }

  public static RiotPlatform defaultPlatform() {
    return EUW1;
  }

  public static RiotPlatform fromFormValue(RiotPlatform value) {
    return value == null ? defaultPlatform() : value;
  }

  public static RiotPlatform fromStoredValue(String value) {
    if (value == null || value.isBlank()) {
      return defaultPlatform();
    }
    return Arrays.stream(values())
        .filter(platform -> platform.name().equalsIgnoreCase(value.trim()))
        .findFirst()
        .orElse(defaultPlatform());
  }
}
