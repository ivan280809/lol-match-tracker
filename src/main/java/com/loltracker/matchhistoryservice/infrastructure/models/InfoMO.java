package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.*;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Info")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
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

  @OneToMany(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "info_id")
  private List<ParticipantMO> participants;

  private String platformId;
  private int queueId;

  @OneToMany(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "info_id")
  private List<TeamMO> teams;

  private String tournamentCode;
}
