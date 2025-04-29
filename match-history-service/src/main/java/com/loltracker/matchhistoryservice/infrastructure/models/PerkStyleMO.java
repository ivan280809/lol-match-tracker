package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.*;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Perk_Style")
@NoArgsConstructor
@AllArgsConstructor
public class PerkStyleMO {
  @Id @GeneratedValue private Long id;

  private String description;

  @OneToMany private List<PerkStyleSelectionMO> selections;
  private int style;
}
