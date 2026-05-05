package com.loltracker.app.ops;

import com.loltracker.app.match.TrackedMatchService;
import com.loltracker.app.notification.NotificationService;
import com.loltracker.app.player.PlayerForm;
import com.loltracker.app.player.PlayerService;
import com.loltracker.app.player.PlayerView;
import com.loltracker.app.player.RiotPlatform;
import com.loltracker.app.settings.AppConfigurationForm;
import com.loltracker.app.settings.AppConfigurationService;
import com.loltracker.app.settings.RiotRegion;
import com.loltracker.app.tracking.PollSummary;
import com.loltracker.app.tracking.PollingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
  private final NotificationService notificationService;
  private final PollRunService pollRunService;
  private final PollingService pollingService;
  private final AppConfigurationService appConfigurationService;
  private final RosterQueryService rosterQueryService;
  private final PlayerPageService playerPageService;
  private final OpsHealthService opsHealthService;
  private final IntegrationOperationsService integrationOperationsService;
  private final AuditService auditService;

  @GetMapping("/")
  public String dashboard(
      @RequestParam(defaultValue = "active") String status,
      @RequestParam(defaultValue = "false") boolean withError,
      @RequestParam(defaultValue = "") String platform,
      @RequestParam(defaultValue = "") String syncStatus,
      @RequestParam(defaultValue = "") String query,
      @RequestParam(defaultValue = "") String champion,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
      @RequestParam(defaultValue = "name") String sort,
      @RequestParam(defaultValue = "player") String tab,
      HttpServletRequest request,
      Model model) {
    populateDashboard(
        model,
        new DashboardFilter(
            status,
            withError,
            parsePlatform(platform),
            syncStatus,
            query,
            champion,
            fromDate,
            toDate,
            sort),
        tab,
        currentPathAndQuery(request));
    return "dashboard";
  }

  @PostMapping("/players")
  public String createPlayer(
      @Valid @ModelAttribute("playerForm") PlayerForm playerForm,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes,
      @RequestParam(defaultValue = "/?tab=player") String returnTo,
      Model model) {
    if (bindingResult.hasErrors()) {
      populateDashboard(model, DashboardFilter.defaultFilter(), "player", safeReturnTo(returnTo));
      return "dashboard";
    }

    try {
      playerService.create(normalizePlayerForm(playerForm));
      redirectAttributes.addFlashAttribute("successMessage", "Jugador creado");
    } catch (RuntimeException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
      redirectAttributes.addFlashAttribute("playerForm", playerForm);
    }
    return "redirect:" + safeReturnTo(returnTo);
  }

  @GetMapping("/players")
  public String playersRedirect() {
    return "redirect:/";
  }

  @GetMapping("/players/{id}")
  public String playerDetail(@PathVariable Long id, Model model) {
    model.addAttribute("detail", playerPageService.getDetail(id));
    return "player-detail";
  }

  @GetMapping("/players/{id}/edit")
  public String editPlayer(@PathVariable Long id, Model model) {
    PlayerView player = playerService.getPlayer(id);
    model.addAttribute("player", player);
    if (!model.containsAttribute("playerForm")) {
      model.addAttribute("playerForm", formFrom(player));
    }
    model.addAttribute("riotPlatforms", RiotPlatform.values());
    return "player-edit";
  }

  @PostMapping("/players/{id}")
  public String updatePlayer(
      @PathVariable Long id,
      @Valid @ModelAttribute("playerForm") PlayerForm playerForm,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes,
      Model model) {
    if (bindingResult.hasErrors()) {
      populateEditModel(id, model);
      return "player-edit";
    }

    try {
      playerService.update(id, normalizePlayerForm(playerForm));
      redirectAttributes.addFlashAttribute("successMessage", "Jugador actualizado");
      return "redirect:/players/" + id;
    } catch (RuntimeException e) {
      bindingResult.reject("player", e.getMessage());
      populateEditModel(id, model);
      return "player-edit";
    }
  }

  @PostMapping("/configuration")
  public String updateConfiguration(
      @Valid @ModelAttribute("configurationForm") AppConfigurationForm configurationForm,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes,
      Model model) {
    if (bindingResult.hasErrors()) {
      populateDashboard(model, DashboardFilter.defaultFilter(), "config", "/?tab=config");
      return "dashboard";
    }

    try {
      appConfigurationService.update(normalizeConfigurationForm(configurationForm));
      redirectAttributes.addFlashAttribute("successMessage", "Configuracion guardada");
    } catch (IllegalArgumentException | IllegalStateException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/?tab=config";
  }

  @PostMapping("/players/{id}/toggle")
  public String togglePlayer(
      @PathVariable Long id,
      @RequestParam boolean active,
      @RequestParam(defaultValue = "/") String returnTo) {
    playerService.setActive(id, active);
    return "redirect:" + safeReturnTo(returnTo);
  }

  @PostMapping("/players/{id}/archive")
  public String archivePlayer(
      @PathVariable Long id,
      @RequestParam(defaultValue = "/") String returnTo,
      RedirectAttributes redirectAttributes) {
    playerService.archive(id);
    redirectAttributes.addFlashAttribute("successMessage", "Jugador archivado");
    return "redirect:" + safeReturnTo(returnTo);
  }

  @PostMapping("/players/{id}/restore")
  public String restorePlayer(
      @PathVariable Long id,
      @RequestParam(defaultValue = "/") String returnTo,
      RedirectAttributes redirectAttributes) {
    playerService.restore(id);
    redirectAttributes.addFlashAttribute("successMessage", "Jugador restaurado");
    return "redirect:" + safeReturnTo(returnTo);
  }

  @PostMapping("/players/validate-account")
  public String validateAccount(
      @Valid @ModelAttribute("playerForm") PlayerForm playerForm,
      BindingResult bindingResult,
      @RequestParam(defaultValue = "/") String returnTo,
      RedirectAttributes redirectAttributes) {
    redirectAttributes.addFlashAttribute("playerForm", playerForm);
    if (bindingResult.hasErrors()) {
      redirectAttributes.addFlashAttribute("errorMessage", "Completa nombre, tag y servidor para validar la cuenta");
      return "redirect:" + safeReturnTo(returnTo);
    }
    IntegrationActionResult result =
        integrationOperationsService.validateAccount(
            playerForm.gameName(), playerForm.tagLine(), playerForm.platform());
    addIntegrationFlash(result, redirectAttributes);
    return "redirect:" + safeReturnTo(returnTo);
  }

  @PostMapping("/polling/run")
  public String runPolling(
      @RequestParam(defaultValue = "/") String returnTo,
      RedirectAttributes redirectAttributes) {
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
    if (summary.playerFailures() > 0 || "SKIPPED".equals(summary.status()) || "RATE_LIMITED".equals(summary.status())) {
      redirectAttributes.addFlashAttribute("errorMessage", message);
    } else {
      redirectAttributes.addFlashAttribute("successMessage", message);
    }
    return "redirect:" + safeReturnTo(returnTo);
  }

  @PostMapping("/integrations/riot/validate")
  public String validateRiotKey(
      @RequestParam(defaultValue = "EUW1") RiotPlatform platform,
      @RequestParam(defaultValue = "/?tab=health") String returnTo,
      RedirectAttributes redirectAttributes) {
    addIntegrationFlash(integrationOperationsService.validateRiotKey(platform), redirectAttributes);
    return "redirect:" + safeReturnTo(returnTo);
  }

  @PostMapping("/integrations/telegram/test")
  public String testTelegram(
      @RequestParam(defaultValue = "/?tab=health") String returnTo,
      RedirectAttributes redirectAttributes) {
    addIntegrationFlash(integrationOperationsService.testTelegram(), redirectAttributes);
    return "redirect:" + safeReturnTo(returnTo);
  }

  @GetMapping("/audit")
  public String audit(Model model) {
    model.addAttribute("audit", auditService.getAudit());
    return "audit";
  }

  private void populateDashboard(Model model, DashboardFilter filter) {
    populateDashboard(model, filter, "player", "/");
  }

  private void populateDashboard(
      Model model, DashboardFilter filter, String activeTab, String currentPathAndQuery) {
    model.addAttribute(
        "dashboard",
        new DashboardView(
            playerService.countPlayers(),
            trackedMatchService.countMatches(),
            notificationService.countPendingNotifications(),
            notificationService.countFailedNotifications()));
    model.addAttribute("players", rosterQueryService.getRoster(filter));
    model.addAttribute("matches", trackedMatchService.getRecentMatches());
    model.addAttribute("runs", pollRunService.getRecentRuns());
    java.util.Optional<PollRunView> activeRun = pollRunService.getActiveRun();
    java.util.Optional<PollRunView> rateLimitRun = pollRunService.getActiveRateLimitPause();
    model.addAttribute("activeRun", activeRun == null ? null : activeRun.orElse(null));
    model.addAttribute("rateLimitRun", rateLimitRun == null ? null : rateLimitRun.orElse(null));
    model.addAttribute("configuration", appConfigurationService.getView());
    model.addAttribute("health", opsHealthService.currentHealth());
    model.addAttribute("filters", filter);
    model.addAttribute("activeTab", sanitizeTab(activeTab));
    model.addAttribute("currentPathAndQuery", currentPathAndQuery);
    model.addAttribute("syncStatuses", List.of("NEW", "SUCCESS", "ERROR"));
    model.addAttribute("sortOptions", List.of("name", "last-error", "last-sync", "rank", "activity"));
    model.addAttribute("riotRegions", RiotRegion.values());
    model.addAttribute("riotPlatforms", RiotPlatform.values());
    if (!model.containsAttribute("playerForm")) {
      model.addAttribute("playerForm", new PlayerForm(RiotPlatform.defaultPlatform(), "", "", true));
    }
    if (!model.containsAttribute("configurationForm")) {
      model.addAttribute("configurationForm", appConfigurationService.getForm());
    }
  }

  private void populateEditModel(Long id, Model model) {
    model.addAttribute("player", playerService.getPlayer(id));
    model.addAttribute("riotPlatforms", RiotPlatform.values());
  }

  private PlayerForm formFrom(PlayerView player) {
    return new PlayerForm(player.platform(), player.gameName(), player.tagLine(), player.active());
  }

  private PlayerForm normalizePlayerForm(PlayerForm form) {
    return new PlayerForm(
        form.platform(),
        form.gameName(),
        form.tagLine(),
        form.active(),
        Boolean.TRUE.equals(form.backfill()));
  }

  private AppConfigurationForm normalizeConfigurationForm(AppConfigurationForm form) {
    return new AppConfigurationForm(
        form.riotApiKey(),
        form.riotRegion(),
        form.telegramBotToken(),
        form.telegramChatId(),
        form.pollingEnabled() == null || Boolean.TRUE.equals(form.pollingEnabled()),
        Boolean.TRUE.equals(form.pollingManualOnly()),
        form.pollingFixedDelay() == null || form.pollingFixedDelay().isBlank()
            ? "PT5M"
            : form.pollingFixedDelay(),
        form.pollingMatchWindowSize() == null ? 10 : form.pollingMatchWindowSize(),
        form.pollingPaginationLimit() == null ? 3 : form.pollingPaginationLimit());
  }

  private void addIntegrationFlash(
      IntegrationActionResult result, RedirectAttributes redirectAttributes) {
    if (result.ok()) {
      redirectAttributes.addFlashAttribute("successMessage", result.summary());
    } else {
      redirectAttributes.addFlashAttribute("errorMessage", result.summary());
    }
  }

  private String safeReturnTo(String returnTo) {
    if (returnTo == null || returnTo.isBlank() || !returnTo.startsWith("/") || returnTo.startsWith("//")) {
      return "/";
    }
    return returnTo;
  }

  private String sanitizeTab(String tab) {
    return switch (tab == null ? "" : tab) {
      case "health", "config", "player" -> tab;
      default -> "player";
    };
  }

  private String currentPathAndQuery(HttpServletRequest request) {
    String uri = request.getRequestURI();
    String query = request.getQueryString();
    return query == null || query.isBlank() ? uri : uri + "?" + query;
  }

  private RiotPlatform parsePlatform(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return RiotPlatform.fromStoredValue(value);
  }
}
