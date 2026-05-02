package com.loltracker.app.settings;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RiotRegionTest {

  @Test
  void regionsExposeRiotRegionalRoutingUrls() {
    assertEquals("https://europe.api.riotgames.com", RiotRegion.EUROPE.baseUrl());
    assertEquals("https://americas.api.riotgames.com", RiotRegion.from("AMERICAS").baseUrl());
    assertEquals("https://asia.api.riotgames.com", RiotRegion.from("asia").baseUrl());
    assertEquals("https://sea.api.riotgames.com", RiotRegion.SEA.baseUrl());
  }
}
