package com.loltracker.app.match;

import com.loltracker.app.player.PlayerEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
    name = "player_matches",
    uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "match_id"}),
    indexes = {
      @Index(name = "idx_player_matches_player_result", columnList = "player_id,result"),
      @Index(name = "idx_player_matches_match", columnList = "match_id"),
      @Index(name = "idx_player_matches_champion", columnList = "champion_name")
    })
@Getter
@Setter
public class PlayerMatchEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "player_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private PlayerEntity player;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "match_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private MatchEntity match;

  @Column(name = "puuid", nullable = false, length = 128)
  private String puuid;

  @Column(name = "champion_id")
  private Integer championId;

  @Column(name = "champion_name", nullable = false, length = 64)
  private String championName;

  @Enumerated(EnumType.STRING)
  @Column(name = "result", nullable = false, length = 16)
  private MatchResult result = MatchResult.UNKNOWN;

  @Column(name = "lane", length = 32)
  private String lane;

  @Column(name = "role", length = 32)
  private String role;

  @Column(name = "kills", nullable = false)
  private int kills;

  @Column(name = "deaths", nullable = false)
  private int deaths;

  @Column(name = "assists", nullable = false)
  private int assists;

  @Column(name = "creep_score", nullable = false)
  private int creepScore;

  @Column(name = "gold_earned", nullable = false)
  private int goldEarned;

  @Column(name = "damage_dealt_to_champions", nullable = false)
  private int damageDealtToChampions;

  @Column(name = "vision_score", nullable = false)
  private int visionScore;

  @Column(name = "notification_suppressed", nullable = false)
  private boolean notificationSuppressed;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
  }
}
