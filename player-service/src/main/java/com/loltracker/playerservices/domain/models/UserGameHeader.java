package com.loltracker.playerservices.domain.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserGameHeader {
  private String puuid;
  private String gameName;
  private String tagLine;
}
