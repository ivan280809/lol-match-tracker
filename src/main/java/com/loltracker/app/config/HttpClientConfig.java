package com.loltracker.app.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

  @Bean
  public RestClient.Builder restClientBuilder(ClientHttpRequestFactory requestFactory) {
    return RestClient.builder().requestFactory(requestFactory);
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

