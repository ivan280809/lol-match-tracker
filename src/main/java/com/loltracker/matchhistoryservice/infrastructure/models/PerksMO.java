package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.*;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Perks")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerksMO {

  @Id @GeneratedValue private Long id;

  @OneToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "statPerks_id")
  private PerkStatsMO statPerks;

  @OneToMany(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "perkStyle_id")
  private List<PerkStyleMO> styles;
}
