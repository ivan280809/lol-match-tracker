package com.loltracker.app.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class HttpClientConfig {

  @Bean
  public RestClient.Builder restClientBuilder(ClientHttpRequestFactory requestFactory) {
    return RestClient.builder().requestFactory(requestFactory);
  }

  /**
   * Provide a default {@link ObjectMapper} bean.
   *
   * <p>The application requires an {@code ObjectMapper} for parsing Riot API
   * JSON responses.  The missing bean caused {@link
   * com.loltracker.app.integration.riot.RiotClient} to fail during application
   * context initialization.  Adding this bean restores the dependency chain
   * while respecting the original design that keeps configuration minimal.
   */
  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  /**
   * Create a {@link ClientHttpRequestFactory} using simple HTTP connections.
   *
   * <p>The configuration properties use a {@code Duration} format.  Spring
   * will resolve the placeholders and convert them to {@link Duration}
   * instances automatically.  If a property is missing a sane default of
   * {@code PT10S} is used.
   */
  @Bean
  public ClientHttpRequestFactory clientHttpRequestFactory(
      @Value("${app.http.connect-timeout:PT10S}") Duration connectTimeout,
      @Value("${app.http.timeout:PT10S}") Duration readTimeout) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    // The setter methods expect milliseconds; convert the {@code Duration}
    // values explicitly to avoid compile‑time type errors.
    requestFactory.setConnectTimeout((int) connectTimeout.toMillis());
    requestFactory.setReadTimeout((int) readTimeout.toMillis());
    return requestFactory;
  }
}

