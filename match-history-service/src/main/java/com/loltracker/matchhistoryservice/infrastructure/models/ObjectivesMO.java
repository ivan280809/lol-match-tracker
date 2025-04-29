package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Objectives")
@NoArgsConstructor
@AllArgsConstructor
public class ObjectivesMO {
  @Id @GeneratedValue private Long id;

  @OneToOne private ObjectiveMO baron;
  @OneToOne private ObjectiveMO champion;
  @OneToOne private ObjectiveMO dragon;
  @OneToOne private ObjectiveMO horde;
  @OneToOne private ObjectiveMO inhibitor;
  @OneToOne private ObjectiveMO riftHerald;
  @OneToOne private ObjectiveMO tower;
}
