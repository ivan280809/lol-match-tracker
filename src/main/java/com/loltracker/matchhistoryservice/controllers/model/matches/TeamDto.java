package com.loltracker.matchhistoryservice.controllers.model.matches;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamDto {
  private List<BanDto> bans;
  private ObjectivesDto objectives;
  private int teamId;
  private boolean win;
}
