package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.*;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Perk_Style")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerkStyleMO {
  @Id @GeneratedValue private Long id;

  private String description;

  @OneToMany(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "perkStyleSelection_id")
  private List<PerkStyleSelectionMO> selections;

  private int style;
}
