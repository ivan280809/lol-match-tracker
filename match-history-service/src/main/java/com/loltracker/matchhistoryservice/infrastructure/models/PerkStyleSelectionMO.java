package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Perk_Style_Selection")
@NoArgsConstructor
@AllArgsConstructor
public class PerkStyleSelectionMO {
  private int perk;
  private int var1;
  private int var2;
  private int var3;
}
