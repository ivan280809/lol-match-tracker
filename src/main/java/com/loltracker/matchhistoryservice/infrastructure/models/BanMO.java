package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Ban")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class BanMO {

  @Id @GeneratedValue private Long id;
  private int championId;
  private int pickTurn;
}
