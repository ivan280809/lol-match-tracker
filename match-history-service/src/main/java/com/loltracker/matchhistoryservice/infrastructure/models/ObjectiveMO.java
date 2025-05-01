package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Objective")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObjectiveMO {

  @Id @GeneratedValue private Long id;

  private boolean first;
  private int kills;
}
