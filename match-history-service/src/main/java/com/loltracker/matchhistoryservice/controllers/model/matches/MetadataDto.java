package com.loltracker.matchhistoryservice.controllers.model.matches;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetadataDto {
  private String dataVersion;
  private String matchId;
  private List<String> participants;
}
