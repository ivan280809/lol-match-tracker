package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Match")
@NoArgsConstructor
@AllArgsConstructor
public class MatchMO {

  @Id @GeneratedValue private Long id;

  @OneToOne private MetadataMO metadata;

  @OneToOne private InfoMO info;
}
