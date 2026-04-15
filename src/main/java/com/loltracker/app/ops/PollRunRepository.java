package com.loltracker.app.ops;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PollRunRepository extends JpaRepository<PollRunEntity, Long> {

  List<PollRunEntity> findTop10ByOrderByStartedAtDesc();
}

