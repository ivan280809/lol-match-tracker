package com.loltracker.matchhistoryservice.controllers.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountDTO {
  private String puuid;
  private String gameName;
  private String tagLine;
}
