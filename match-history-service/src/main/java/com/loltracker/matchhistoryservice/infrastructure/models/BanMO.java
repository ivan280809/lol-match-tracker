package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Ban")
@NoArgsConstructor
@AllArgsConstructor
public class BanMO {
  private int championId;
  private int pickTurn;
}
