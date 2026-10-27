package com.loltracker.app.integration.telegram;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loltracker.app.ops.OpsMetrics;
import com.loltracker.app.settings.AppConfigurationService;
import com.loltracker.app.settings.RuntimeAppConfiguration;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class TelegramSecretSafetyTest {
  private static final String TOKEN = "fixture-private-token";
  private static final String URL = "https://api.telegram.org/bot" + TOKEN + "/sendMessage";

  private TelegramNotifier notifier(RestClient.Builder builder) {
    AppConfigurationService configuration = mock(AppConfigurationService.class);
    when(configuration.getRuntimeConfiguration())
        .thenReturn(new RuntimeAppConfiguration("fixture-riot", null, TOKEN, "fixture-chat"));
    TelegramNotifier notifier = new TelegramNotifier(builder, configuration, new ObjectMapper(), mock(OpsMetrics.class));
    ReflectionTestUtils.setField(notifier, "telegramApiBaseUrl", "https://api.telegram.org");
    ReflectionTestUtils.setField(notifier, "httpMaxAttempts", 1);
    return notifier;
  }

  @Test
  void stripsTokenBodyAndCauseWhilePreservingRateLimitMetadata() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
        .contentType(MediaType.APPLICATION_JSON).header("Retry-After", "1200")
        .header("X-Debug", TOKEN)
        .body("{\"description\":\"" + TOKEN + "\",\"parameters\":{\"retry_after\":1500}}"));
    RestClientResponseException failure = assertThrows(RestClientResponseException.class,
        () -> notifier(builder).send("fixture"));
    assertEquals(429, failure.getStatusCode().value());
    assertEquals("1200", failure.getResponseHeaders().getFirst("Retry-After"));
    assertEquals("{\"parameters\":{\"retry_after\":1500}}", failure.getResponseBodyAsString());
    assertNull(failure.getResponseHeaders().getFirst("X-Debug"));
    assertNull(failure.getCause());
    assertFalse(stackTrace(failure).contains(TOKEN));
    assertFalse(stackTrace(failure).contains("api.telegram.org"));
    server.verify();
  }

  @Test
  void transportFailureCannotLeakTokenThroughNestedCause() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo(URL)).andRespond(request -> {
      throw new ResourceAccessException("Connection failed for " + URL, new java.io.IOException(TOKEN));
    });
    ResourceAccessException failure = assertThrows(ResourceAccessException.class,
        () -> notifier(builder).send("fixture"));
    assertNull(failure.getCause());
    assertFalse(stackTrace(failure).contains(TOKEN));
    server.verify();
  }

  private String stackTrace(Exception failure) {
    StringWriter output = new StringWriter();
    failure.printStackTrace(new PrintWriter(output));
    return output.toString();
  }
}
