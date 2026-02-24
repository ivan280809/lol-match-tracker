package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Perk_Style_Selection")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerkStyleSelectionMO {

  @Id @GeneratedValue private Long id;

  private int perk;
  private int var1;
  private int var2;
  private int var3;
}
