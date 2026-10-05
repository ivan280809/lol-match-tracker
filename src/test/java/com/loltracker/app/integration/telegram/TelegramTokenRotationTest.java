package com.loltracker.app.integration.telegram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.loltracker.app.settings.AppConfigurationService;
import com.loltracker.app.settings.RuntimeAppConfiguration;
import com.loltracker.app.settings.RiotRegion;
import com.loltracker.app.ops.OpsMetrics;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Test that TelegramNotifier rotates its bot token and chat id between consecutive calls.
 * It uses a mocked {@link AppConfigurationService} that returns a different configuration on
 * each call to {@link AppConfigurationService#getRuntimeConfiguration()}.
 */
class TelegramTokenRotationTest {

  @Test
  void rotatesBotTokenAndChatIdBetweenCalls() throws Exception {
    // Arrange
    AppConfigurationService configService = mock(AppConfigurationService.class);
    RuntimeAppConfiguration first =
        new RuntimeAppConfiguration("riot-key", RiotRegion.EUROPE, "token-a", "chat-a");
    RuntimeAppConfiguration second =
        new RuntimeAppConfiguration("riot-key", RiotRegion.EUROPE, "token-b", "chat-b");
    when(configService.getRuntimeConfiguration()).thenReturn(first).thenReturn(second);

    RestClient.Builder restClientBuilder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();

    OpsMetrics opsMetrics = mock(OpsMetrics.class);
    TelegramNotifier notifier = new TelegramNotifier(restClientBuilder, configService,
        new ObjectMapper(), opsMetrics);

    // Expect first request with token-a and chat-a
    server.expect(requestTo("https://api.telegram.org/bottoken-a/sendMessage"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":123}}", MediaType.APPLICATION_JSON));

    // Expect second request with token-b and chat-b
    server.expect(requestTo("https://api.telegram.org/bottoken-b/sendMessage"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":123}}", MediaType.APPLICATION_JSON));

    // Act & Assert
    TelegramDeliveryReceipt receipt1 = notifier.send("Hello 1");
    assertEquals(123, receipt1.messageId());

    TelegramDeliveryReceipt receipt2 = notifier.send("Hello 2");
    assertEquals(123, receipt2.messageId());

    server.verify();
  }
}
