package com.loltracker.app.player;

import java.util.Arrays;

public enum RiotPlatform {
  BR1("BR", "Brazil", "https://br1.api.riotgames.com"),
  EUN1("EUNE", "Europe Nordic & East", "https://eun1.api.riotgames.com"),
  EUW1("EUW", "Europe West", "https://euw1.api.riotgames.com"),
  JP1("JP", "Japan", "https://jp1.api.riotgames.com"),
  KR("KR", "Korea", "https://kr.api.riotgames.com"),
  LA1("LAN", "Latin America North", "https://la1.api.riotgames.com"),
  LA2("LAS", "Latin America South", "https://la2.api.riotgames.com"),
  NA1("NA", "North America", "https://na1.api.riotgames.com"),
  OC1("OCE", "Oceania", "https://oc1.api.riotgames.com"),
  TR1("TR", "Turkey", "https://tr1.api.riotgames.com"),
  RU("RU", "Russia", "https://ru.api.riotgames.com"),
  PH2("PH", "Philippines", "https://ph2.api.riotgames.com"),
  SG2("SG", "Singapore", "https://sg2.api.riotgames.com"),
  TH2("TH", "Thailand", "https://th2.api.riotgames.com"),
  TW2("TW", "Taiwan", "https://tw2.api.riotgames.com"),
  VN2("VN", "Vietnam", "https://vn2.api.riotgames.com");

  private final String shortName;
  private final String displayName;
  private final String baseUrl;

  RiotPlatform(String shortName, String displayName, String baseUrl) {
    this.shortName = shortName;
    this.displayName = displayName;
    this.baseUrl = baseUrl;
  }

  public String shortName() {
    return shortName;
  }

  public String displayName() {
    return displayName;
  }

  public String baseUrl() {
    return baseUrl;
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
