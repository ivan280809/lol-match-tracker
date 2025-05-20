package com.loltracker.playerservices.infrastructure.models.matches;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjectiveDto {
  private boolean first;
  private int kills;
}
