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

  @OneToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "baron_id")
  private ObjectiveMO baron;

  @OneToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "champion_id")
  private ObjectiveMO champion;

  @OneToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "dragon_id")
  private ObjectiveMO dragon;

  @OneToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "horde_id")
  private ObjectiveMO horde;

  @OneToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "inhibitor_id")
  private ObjectiveMO inhibitor;

  @OneToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "rift_herald_id")
  private ObjectiveMO riftHerald;

  @OneToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "tower_id")
  private ObjectiveMO tower;
}
