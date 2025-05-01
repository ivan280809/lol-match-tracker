package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.*;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Team")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class TeamMO {

  @Id @GeneratedValue private Long id;

  @OneToMany
  @JoinColumn(name = "team_id")
  private List<BanMO> bans;

  @OneToOne
  @JoinColumn(name = "objectives_id")
  private ObjectivesMO objectives;

  private int teamId;
  private boolean win;
}
