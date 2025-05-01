package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.*;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Matches")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MatchesMO {

  @Id @GeneratedValue private Long id;

  @OneToMany private List<MatchMO> matches;
}
