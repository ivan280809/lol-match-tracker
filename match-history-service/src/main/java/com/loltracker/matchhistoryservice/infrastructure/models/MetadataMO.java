package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Metadata")
@NoArgsConstructor
@AllArgsConstructor
public class MetadataMO {

  @Id @GeneratedValue private Long id;

  private String dataVersion;
  private String matchId;
  private List<String> participants;
}
