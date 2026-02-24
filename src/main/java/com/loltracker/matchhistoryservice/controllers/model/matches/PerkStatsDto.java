package com.loltracker.matchhistoryservice.controllers.model.matches;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PerkStatsDto {
  private int defense;
  private int flex;
  private int offense;
}
