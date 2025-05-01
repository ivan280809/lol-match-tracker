package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Objectives")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ObjectivesMO {
  @Id @GeneratedValue private Long id;

  @OneToOne
  @JoinColumn(name = "baron_id")
  private ObjectiveMO baron;

  @OneToOne
  @JoinColumn(name = "champion_id")
  private ObjectiveMO champion;

  @OneToOne
  @JoinColumn(name = "dragon_id")
  private ObjectiveMO dragon;

  @OneToOne
  @JoinColumn(name = "horde_id")
  private ObjectiveMO horde;

  @OneToOne
  @JoinColumn(name = "inhibitor_id")
  private ObjectiveMO inhibitor;

  @OneToOne
  @JoinColumn(name = "rift_herald_id")
  private ObjectiveMO riftHerald;

  @OneToOne
  @JoinColumn(name = "tower_id")
  private ObjectiveMO tower;
}
