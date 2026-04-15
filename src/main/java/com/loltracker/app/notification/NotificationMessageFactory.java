package com.loltracker.app.notification;

import com.loltracker.app.match.TrackedMatchEntity;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageFactory {

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("Europe/Madrid"));

  public String build(TrackedMatchEntity match) {
    return "Jugador: "
        + match.getPlayer().getGameName()
        + "#"
        + match.getPlayer().getTagLine()
        + "\nCampeon: "
        + match.getChampionName()
        + "\nResultado: "
        + match.getResult()
        + "\nModo: "
        + match.getGameMode()
        + "\nDuracion (s): "
        + match.getDurationSeconds()
        + "\nFin: "
        + FORMATTER.format(match.getGameEndAt());
  }
}

