package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Match")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MatchMO {

  @Id @GeneratedValue private Long id;

  @OneToOne
  @JoinColumn(name = "metadata_id")
  private MetadataMO metadata;

  @OneToOne
  @JoinColumn(name = "info_id")
  private InfoMO info;
}
