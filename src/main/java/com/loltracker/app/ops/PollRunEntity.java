package com.loltracker.app.ops;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "poll_runs")
@Getter
@Setter
public class PollRunEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Column(name = "status", nullable = false, length = 16)
  private String status;

  @Column(name = "players_processed", nullable = false)
  private int playersProcessed;

  @Column(name = "new_matches_found", nullable = false)
  private int newMatchesFound;

  @Column(name = "notifications_sent", nullable = false)
  private int notificationsSent;

  @Column(name = "error_summary", length = 500)
  private String errorSummary;
}

