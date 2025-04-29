package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Team")
@NoArgsConstructor
@AllArgsConstructor
public class TeamMO {

  @Id @GeneratedValue private Long id;

  private List<BanMO> bans;
  private ObjectivesMO objectives;
  private int teamId;
  private boolean win;
}
