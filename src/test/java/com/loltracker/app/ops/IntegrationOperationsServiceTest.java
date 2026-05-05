package com.loltracker.app.ops;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.loltracker.app.integration.riot.RiotAccount;
import com.loltracker.app.integration.riot.RiotApiException;
import com.loltracker.app.integration.riot.RiotClient;
import com.loltracker.app.integration.riot.RiotErrorCategory;
import com.loltracker.app.integration.telegram.TelegramDeliveryReceipt;
import com.loltracker.app.integration.telegram.TelegramNotifier;
import com.loltracker.app.player.RiotPlatform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntegrationOperationsServiceTest {

  @Mock private RiotClient riotClient;
  @Mock private TelegramNotifier telegramNotifier;
  @Mock private ExternalCallLogService externalCallLogService;

  @InjectMocks private IntegrationOperationsService integrationOperationsService;

  @Test
  void validateAccountRecordsOk() {
    when(riotClient.fetchAccount("Bazaga", "ESP"))
        .thenReturn(new RiotAccount("puuid-1", "Bazaga", "ESP"));

    IntegrationActionResult result =
        integrationOperationsService.validateAccount("Bazaga", "ESP", RiotPlatform.EUW1);

    assertTrue(result.ok());
    assertEquals("OK", result.category());
    assertTrue(result.summary().contains("Cuenta Riot OK"));
    verify(externalCallLogService).recordOk(eq("RIOT"), eq("VALIDATE_ACCOUNT"), contains("Bazaga#ESP"));
  }

  @Test
  void validateRiotKeyRecordsFriendlyFailure() {
    when(riotClient.validateApiKey(RiotPlatform.EUW1))
        .thenThrow(new RiotApiException(RiotErrorCategory.UNAUTHORIZED, "Riot API key no autorizada o caducada"));

    IntegrationActionResult result = integrationOperationsService.validateRiotKey(RiotPlatform.EUW1);

    assertFalse(result.ok());
    assertEquals("UNAUTHORIZED", result.category());
    verify(externalCallLogService)
        .recordError(eq("RIOT"), eq("VALIDATE_KEY"), eq("UNAUTHORIZED"), contains("no autorizada"));
  }

  @Test
  void testTelegramRecordsReceipt() {
    when(telegramNotifier.send(anyString())).thenReturn(new TelegramDeliveryReceipt(99));

    IntegrationActionResult result = integrationOperationsService.testTelegram();

    assertTrue(result.ok());
    assertTrue(result.summary().contains("mensaje 99"));
    verify(externalCallLogService).recordOk(eq("TELEGRAM"), eq("TEST_SEND"), contains("99"));
  }
}
