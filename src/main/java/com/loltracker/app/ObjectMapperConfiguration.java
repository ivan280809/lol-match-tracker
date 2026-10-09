package com.loltracker.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration to provide a Jackson {@link ObjectMapper} bean.
 *
 * <p>Spring Boot 3+ automatically configures a Jackson ObjectMapper via
 * {@link org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration},
 * but some components in this project explicitly require an instance of
 * {@link ObjectMapper} that has modules registered (e.g. Java time, data
 * formats). The original implementation was commented out, leading to
 * missing bean errors in integration tests. This configuration explicitly
 * creates a Jackson 2 mapper using the {@link JsonMapper#builder()} API
 * which automatically discovers and registers modules on the classpath.
 */
@Configuration
public class ObjectMapperConfiguration {

  /**
   * Create a Jackson {@link ObjectMapper} bean that is module-aware.
   *
   * @return a configured ObjectMapper instance
   */
  @Bean
  public ObjectMapper objectMapper() {
    return JsonMapper.builder().findAndAddModules().build();
  }
}
