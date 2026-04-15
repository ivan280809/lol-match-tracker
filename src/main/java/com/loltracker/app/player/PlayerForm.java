package com.loltracker.app.player;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlayerForm(
    @NotBlank @Size(max = 64) String gameName,
    @NotBlank @Size(max = 32) String tagLine,
    boolean active) {}

