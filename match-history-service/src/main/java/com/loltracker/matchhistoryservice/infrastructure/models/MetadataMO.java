package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.*;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Metadata")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetadataMO {

  @Id @GeneratedValue private Long id;

  private String dataVersion;
  private String matchId;

  @ElementCollection
  @CollectionTable(name = "metadata_participants", joinColumns = @JoinColumn(name = "metadata_id"))
  @Column(name = "participant_id")
  private List<String> participants;
}
