package com.loltracker.app.integration.telegram;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.loltracker.app.ops.OpsMetrics;
import com.loltracker.app.settings.AppConfigurationService;
import com.loltracker.app.settings.RuntimeAppConfiguration;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;
// Jackson 2.15+ provides `com.fasterxml.jackson.databind.json.JsonMapper`.  The
// previous `tools.jackson` package was part of the Spring Boot 4.0 pre‑relases.
// Switching to the official Jackson package ensures the test compiles against
// the current dependency set.
import com.fasterxml.jackson.databind.json.JsonMapper;

class TelegramNotifierDeadlineTest {

  @Test
  void boundsWholeTelegramResponseEvenWhenServerKeepsTricklingBytes() throws Exception {
    ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setExecutor(serverExecutor);
    server.createContext(
        "/",
        exchange -> {
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, 0);
          try (OutputStream body = exchange.getResponseBody()) {
            for (int index = 0; index < 30; index++) {
              body.write(" ".getBytes(StandardCharsets.UTF_8));
              body.flush();
              Thread.sleep(100);
            }
          } catch (Exception ignored) {
            // The bounded client closes the response stream when its deadline expires.
          }
        });
    server.start();
    try {
      AppConfigurationService configurationService = mock(AppConfigurationService.class);
      when(configurationService.getRuntimeConfiguration())
          .thenReturn(new RuntimeAppConfiguration("fixture-key", null, "fixture-token", "fixture-chat"));
      TelegramNotifier notifier =
          new TelegramNotifier(RestClient.builder(), configurationService, JsonMapper.builder().build(), mock(OpsMetrics.class));
      ReflectionTestUtils.setField(notifier, "telegramApiBaseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
      ReflectionTestUtils.setField(notifier, "httpTimeout", Duration.ofSeconds(1));
      ReflectionTestUtils.setField(notifier, "httpMaxAttempts", 1);

      long started = System.nanoTime();
      assertThrows(RestClientException.class, () -> notifier.send("fixture message", Duration.ofMillis(300)));
      long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

      assertTrue(elapsedMillis < 1_500, "request exceeded its 300ms response deadline: " + elapsedMillis + "ms");
    } finally {
      server.stop(0);
      serverExecutor.shutdownNow();
    }
  }
}
