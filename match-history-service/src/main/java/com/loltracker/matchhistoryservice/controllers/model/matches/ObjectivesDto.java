package com.loltracker.matchhistoryservice.controllers.model.matches;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjectivesDto {
  private ObjectiveDto baron;
  private ObjectiveDto champion;
  private ObjectiveDto dragon;
  private ObjectiveDto horde;
  private ObjectiveDto inhibitor;
  private ObjectiveDto riftHerald;
  private ObjectiveDto tower;
}
