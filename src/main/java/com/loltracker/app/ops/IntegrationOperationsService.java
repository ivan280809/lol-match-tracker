package com.loltracker.app.ops;

import com.loltracker.app.integration.riot.RiotAccount;
import com.loltracker.app.integration.riot.RiotApiException;
import com.loltracker.app.integration.riot.RiotClient;
import com.loltracker.app.integration.telegram.TelegramDeliveryReceipt;
import com.loltracker.app.integration.telegram.TelegramNotifier;
import com.loltracker.app.player.RiotPlatform;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IntegrationOperationsService {

  private final RiotClient riotClient;
  private final TelegramNotifier telegramNotifier;
  private final ExternalCallLogService externalCallLogService;

  public IntegrationActionResult validateRiotKey(RiotPlatform platform) {
    try {
      String summary = riotClient.validateApiKey(RiotPlatform.fromFormValue(platform));
      externalCallLogService.recordOk("RIOT", "VALIDATE_KEY", summary);
      return new IntegrationActionResult(true, "OK", summary);
    } catch (RuntimeException e) {
      IntegrationActionResult result = failureResult(e, "No se pudo validar Riot");
      externalCallLogService.recordError("RIOT", "VALIDATE_KEY", result.category(), result.summary());
      return result;
    }
  }

  public IntegrationActionResult validateAccount(
      String gameName, String tagLine, RiotPlatform platform) {
    String displayName = safeDisplay(gameName, tagLine);
    try {
      RiotAccount account = riotClient.fetchAccount(gameName.trim(), tagLine.trim());
      String summary =
          "Cuenta Riot OK: "
              + account.gameName()
              + "#"
              + account.tagLine()
              + " en "
              + RiotPlatform.fromFormValue(platform).shortName();
      externalCallLogService.recordOk("RIOT", "VALIDATE_ACCOUNT", summary);
      return new IntegrationActionResult(true, "OK", summary);
    } catch (RuntimeException e) {
      IntegrationActionResult result = failureResult(e, "No se pudo validar " + displayName);
      externalCallLogService.recordError("RIOT", "VALIDATE_ACCOUNT", result.category(), result.summary());
      return result;
    }
  }

  public IntegrationActionResult testTelegram() {
    try {
      TelegramDeliveryReceipt receipt =
          telegramNotifier.send("LOL Match Tracker test - " + Instant.now());
      String suffix = receipt == null || receipt.messageId() == null ? "" : " (mensaje " + receipt.messageId() + ")";
      String summary = "Telegram OK" + suffix;
      externalCallLogService.recordOk("TELEGRAM", "TEST_SEND", summary);
      return new IntegrationActionResult(true, "OK", summary);
    } catch (RuntimeException e) {
      IntegrationActionResult result = failureResult(e, "No se pudo enviar el test de Telegram");
      externalCallLogService.recordError("TELEGRAM", "TEST_SEND", result.category(), result.summary());
      return result;
    }
  }

  private IntegrationActionResult failureResult(RuntimeException e, String fallback) {
    String category = e instanceof RiotApiException riotApiException ? riotApiException.category().name() : e.getClass().getSimpleName();
    String message = e.getMessage();
    if (message == null || message.isBlank()) {
      message = fallback;
    }
    return new IntegrationActionResult(false, category, message.substring(0, Math.min(500, message.length())));
  }

  private String safeDisplay(String gameName, String tagLine) {
    String name = gameName == null ? "" : gameName.trim();
    String tag = tagLine == null ? "" : tagLine.trim();
    if (name.isBlank() && tag.isBlank()) {
      return "la cuenta Riot";
    }
    return name + "#" + tag;
  }
}
