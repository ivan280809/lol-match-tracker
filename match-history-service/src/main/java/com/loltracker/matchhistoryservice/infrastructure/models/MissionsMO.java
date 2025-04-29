package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Missions")
@NoArgsConstructor
@AllArgsConstructor
public class MissionsMO {

  @Id @GeneratedValue private Long id;

  private int playerScore0;
  private int playerScore1;
  private int playerScore2;
  private int playerScore3;
  private int playerScore4;
  private int playerScore5;
  private int playerScore6;
  private int playerScore7;
  private int playerScore8;
  private int playerScore9;
  private int playerScore10;
  private int playerScore11;
}
