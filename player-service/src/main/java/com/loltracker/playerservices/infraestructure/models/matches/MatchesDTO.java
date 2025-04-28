package com.loltracker.playerservices.infraestructure.models.matches;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchesDTO {
  List<String> matches;
}
