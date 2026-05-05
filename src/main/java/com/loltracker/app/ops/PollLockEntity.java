package com.loltracker.app.ops;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "poll_locks")
@Getter
@Setter
public class PollLockEntity {

  @Id
  @Column(name = "name", length = 64)
  private String name;

  @Column(name = "owner", length = 128)
  private String owner;

  @Column(name = "locked_until")
  private Instant lockedUntil;

  @Column(name = "acquired_at")
  private Instant acquiredAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Version
  @Column(name = "version")
  private Long version;
}
