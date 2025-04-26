package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class LolPlayerHeaderMO {
  @Id private String puuid;

  private String gameName;
  private String tagLine;
}
