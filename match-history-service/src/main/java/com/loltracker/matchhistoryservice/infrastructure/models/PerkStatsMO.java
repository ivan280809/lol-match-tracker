package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Perk_Stats")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerkStatsMO {
  @Id @GeneratedValue private Long id;

  private int defense;
  private int flex;
  private int offense;
}
