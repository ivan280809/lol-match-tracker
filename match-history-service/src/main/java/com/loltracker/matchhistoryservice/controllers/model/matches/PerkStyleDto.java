package com.loltracker.matchhistoryservice.controllers.model.matches;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PerkStyleDto {
  private String description;
  private List<PerkStyleSelectionDto> selections;
  private int style;
}
