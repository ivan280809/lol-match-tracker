package com.loltracker.playerservices.domain.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Account {
  private String puuid;
  private String gameName;
  private String tagLine;
}
