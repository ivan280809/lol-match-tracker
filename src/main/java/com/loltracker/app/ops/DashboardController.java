package com.loltracker.app.ops;

import com.loltracker.app.match.TrackedMatchService;
import com.loltracker.app.player.PlayerForm;
import com.loltracker.app.player.PlayerService;
import com.loltracker.app.tracking.PollSummary;
import com.loltracker.app.tracking.PollingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class DashboardController {

  private final PlayerService playerService;
  private final TrackedMatchService trackedMatchService;
  private final PollRunService pollRunService;
  private final PollingService pollingService;

  @GetMapping("/")
  public String dashboard(Model model) {
    model.addAttribute(
        "dashboard", new DashboardView(playerService.countPlayers(), trackedMatchService.countMatches()));
    model.addAttribute("players", playerService.getAllPlayers());
    model.addAttribute("matches", trackedMatchService.getRecentMatches());
    model.addAttribute("runs", pollRunService.getRecentRuns());
    if (!model.containsAttribute("playerForm")) {
      model.addAttribute("playerForm", new PlayerForm("", "", true));
    }
    return "dashboard";
  }

  @PostMapping("/players")
  public String createPlayer(
      @Valid @ModelAttribute("playerForm") PlayerForm playerForm,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes,
      Model model) {
    if (bindingResult.hasErrors()) {
      model.addAttribute(
          "dashboard", new DashboardView(playerService.countPlayers(), trackedMatchService.countMatches()));
      model.addAttribute("players", playerService.getAllPlayers());
      model.addAttribute("matches", trackedMatchService.getRecentMatches());
      model.addAttribute("runs", pollRunService.getRecentRuns());
      return "dashboard";
    }

    try {
      playerService.create(playerForm);
      redirectAttributes.addFlashAttribute("successMessage", "Jugador creado");
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/";
  }

  @PostMapping("/players/{id}/toggle")
  public String togglePlayer(@PathVariable Long id, @RequestParam boolean active) {
    playerService.setActive(id, active);
    return "redirect:/";
  }

  @PostMapping("/polling/run")
  public String runPolling(RedirectAttributes redirectAttributes) {
    PollSummary summary = pollingService.runPoll();
    String message =
        "Polling ejecutado. Estado: "
            + summary.status()
            + ", jugadores: "
            + summary.playersProcessed()
            + ", nuevas partidas: "
            + summary.newMatchesFound()
            + ", avisos: "
            + summary.notificationsSent();
    if (summary.playerFailures() > 0) {
      redirectAttributes.addFlashAttribute("errorMessage", message);
    } else {
      redirectAttributes.addFlashAttribute("successMessage", message);
    }
    return "redirect:/";
  }
}
