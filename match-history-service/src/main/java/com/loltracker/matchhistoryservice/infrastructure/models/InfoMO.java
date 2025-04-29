package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Info")
@NoArgsConstructor
@AllArgsConstructor
public class InfoMO {

  @Id @GeneratedValue private Long id;

  private String endOfGameResult;
  private long gameCreation;
  private long gameDuration;
  private long gameEndTimestamp;
  private long gameId;
  private String gameMode;
  private String gameName;
  private long gameStartTimestamp;
  private String gameType;
  private String gameVersion;
  private int mapId;
  private List<ParticipantMO> participants;
  private String platformId;
  private int queueId;
  private List<TeamMO> teams;
  private String tournamentCode;
}
