package com.loltracker.app.ops;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.loltracker.app.integration.riot.RiotAccount;
import com.loltracker.app.integration.riot.RiotClient;
import com.loltracker.app.integration.telegram.TelegramNotifier;
import com.loltracker.lolmatchtracker.LolMatchTrackerApplication;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = LolMatchTrackerApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"server.address=127.0.0.1", "app.notification.dispatch.delay=PT1H"})
@DirtiesContext
@TestPropertySource(locations = {"file:src/main/resources/application.properties", "classpath:application.properties"})
class DashboardRedirectHttpIntegrationTest {
  @Value("${local.server.port}") int port;
  @MockitoBean RiotClient riot;
  @MockitoBean TelegramNotifier telegram;

  @Test
  void firstFormSubmissionKeepsFlashAndReturnsToDashboardWithoutUrlSession() throws Exception {
    when(riot.isConfigured()).thenReturn(true);
    when(riot.fetchAccount("RedirectPlayer", "TEST"))
        .thenReturn(new RiotAccount("redirect-puuid", "RedirectPlayer", "TEST"));
    HttpClient client = HttpClient.newBuilder()
        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
        .followRedirects(HttpClient.Redirect.NEVER).build();
    URI root = URI.create("http://127.0.0.1:" + port + "/");
    HttpRequest request = HttpRequest.newBuilder(root.resolve("players"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(
            "gameName=RedirectPlayer&tagLine=TEST&platform=EUW1&active=true&returnTo=%2F%3Ftab%3Dplayer"))
        .build();
    HttpResponse<String> posted = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(302, posted.statusCode());
    String location = posted.headers().firstValue("location").orElseThrow();
    assertFalse(location.contains(";jsessionid="), "Session identifiers must not enter redirect URLs");
    HttpResponse<String> dashboard = client.send(
        HttpRequest.newBuilder(root.resolve(location)).GET().build(), HttpResponse.BodyHandlers.ofString());
    assertEquals(200, dashboard.statusCode());
    assertTrue(dashboard.body().contains("Jugador creado"));
    assertTrue(dashboard.body().contains("RedirectPlayer"));
    verify(riot).fetchAccount("RedirectPlayer", "TEST");
    verifyNoInteractions(telegram);
  }
}
