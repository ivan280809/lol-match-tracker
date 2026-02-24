package com.loltracker.playerservices.infrastructure.models.matches;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PerksDto {
  private PerkStatsDto statPerks;
  private List<PerkStyleDto> styles;
}
