package com.loltracker.playerservices.domain.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerJson {
  private String summonerName;
  private String tagLine;
}
