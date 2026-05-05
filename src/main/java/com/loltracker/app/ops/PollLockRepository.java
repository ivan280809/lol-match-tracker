package com.loltracker.app.ops;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PollLockRepository extends JpaRepository<PollLockEntity, String> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select pl from PollLockEntity pl where pl.name = :name")
  Optional<PollLockEntity> findByNameForUpdate(@Param("name") String name);
}
