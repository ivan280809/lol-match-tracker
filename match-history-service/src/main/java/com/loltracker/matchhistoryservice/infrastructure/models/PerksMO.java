package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.*;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Perks")
@NoArgsConstructor
@AllArgsConstructor
public class PerksMO {

  @Id @GeneratedValue private Long id;

  @OneToOne private PerkStatsMO statPerks;
  private List<PerkStyleMO> styles;
}
