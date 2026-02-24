package com.loltracker.notificationservice.controller;

import com.loltracker.notificationservice.domain.TelegramService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class MatchNotificationController {

  private final TelegramService telegramService;

  @PostMapping
  public ResponseEntity<String> sendMessage(@RequestBody Map<String, Object> payload) {
    String text = payload.toString();
    telegramService.sendText(text);
    return ResponseEntity.ok("Mensaje enviado a Telegram");
  }
}
