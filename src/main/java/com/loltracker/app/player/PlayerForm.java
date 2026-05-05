package com.loltracker.app.player;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlayerForm(
    RiotPlatform platform,
    @NotBlank @Size(max = 64) String gameName,
    @NotBlank @Size(max = 32) String tagLine,
    boolean active,
    Boolean backfill) {

  public PlayerForm(String gameName, String tagLine, boolean active) {
    this(null, gameName, tagLine, active, false);
  }

  public PlayerForm(RiotPlatform platform, String gameName, String tagLine, boolean active) {
    this(platform, gameName, tagLine, active, false);
  }
}

