package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Objective")
@NoArgsConstructor
@AllArgsConstructor
public class ObjectiveMO {
  private boolean first;
  private int kills;
}
