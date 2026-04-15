package com.loltracker.app.player;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerApiController {

  private final PlayerService playerService;

  @GetMapping
  public List<PlayerView> getPlayers() {
    return playerService.getAllPlayers();
  }

  @GetMapping("/{id}")
  public PlayerView getPlayer(@PathVariable Long id) {
    return playerService.getPlayer(id);
  }

  @PostMapping
  public PlayerView createPlayer(@Valid @RequestBody PlayerForm form) {
    return playerService.create(form);
  }

  @PutMapping("/{id}")
  public PlayerView updatePlayer(@PathVariable Long id, @Valid @RequestBody PlayerForm form) {
    return playerService.update(id, form);
  }

  @PostMapping("/{id}/activation")
  public void setActivation(@PathVariable Long id, @RequestParam boolean active) {
    playerService.setActive(id, active);
  }
}

