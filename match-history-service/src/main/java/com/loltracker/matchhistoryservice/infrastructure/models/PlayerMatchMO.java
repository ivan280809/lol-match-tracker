package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "player_match")
@NoArgsConstructor
@AllArgsConstructor
public class PlayerMatchMO {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String puuid;
  private String matchId;

  public PlayerMatchMO(String puuid, String match) {
    this.puuid = puuid;
    this.matchId = match;
  }
}
