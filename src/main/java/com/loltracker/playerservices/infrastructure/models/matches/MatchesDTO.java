package com.loltracker.playerservices.infrastructure.models.matches;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchesDTO {
  List<MatchDto> matches;
}
