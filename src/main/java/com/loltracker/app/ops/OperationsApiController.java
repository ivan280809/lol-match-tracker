package com.loltracker.app.ops;

import com.loltracker.app.match.TrackedMatchService;
import com.loltracker.app.match.TrackedMatchView;
import com.loltracker.app.tracking.PollSummary;
import com.loltracker.app.tracking.PollingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operations")
@RequiredArgsConstructor
public class OperationsApiController {

  private final PollingService pollingService;
  private final PollRunService pollRunService;
  private final TrackedMatchService trackedMatchService;

  @GetMapping("/runs")
  public List<PollRunView> getRecentRuns() {
    return pollRunService.getRecentRuns();
  }

  @GetMapping("/matches")
  public List<TrackedMatchView> getRecentMatches() {
    return trackedMatchService.getRecentMatches();
  }

  @PostMapping("/poll")
  public PollSummary runPoll() {
    return pollingService.runPoll();
  }
}

