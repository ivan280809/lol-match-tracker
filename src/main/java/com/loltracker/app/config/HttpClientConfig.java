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

  @Bean
  public ClientHttpRequestFactory clientHttpRequestFactory(
      @Value("${app.http.connect-timeout:${app.http.timeout:PT10S}}") Duration connectTimeout,
      @Value("${app.http.timeout:PT10S}") Duration readTimeout) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(connectTimeout);
    requestFactory.setReadTimeout(readTimeout);
    return requestFactory;
  }
}

