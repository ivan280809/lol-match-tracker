package com.loltracker.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration to provide a Jackson {@link ObjectMapper} bean.
 *
 * <p>Spring Boot 4+ automatically configures a Jackson {@code ObjectMapper}
 * instance.  However the project uses a Jackson 2 mapper in several
 * components (e.g. {@link com.loltracker.app.integration.riot.RiotClient}).
 * The default Boot mapper is Jackson 3 and does not expose the same API
 * surface as Jackson 2.  To preserve compatibility we expose an explicitly
 * configured Jackson 2 mapper that registers all modules found on the
 * classpath.
 *
 * <p>This bean is declared with {@link org.springframework.context.annotation.Primary}
 * so it will be used when autowiring {@code ObjectMapper} directly.  The
 * Spring Boot default (Jackson 3) remains available under its own bean
 * name, ensuring no other components are affected.
 */
@Configuration
public class ObjectMapperConfiguration {

  /**
   * Provide a module‑aware Jackson 2 {@code ObjectMapper}.
   *
   * @return the configured {@code ObjectMapper}
   */
  @Bean(name = "jackson2ObjectMapper")
  @org.springframework.context.annotation.Primary
  public ObjectMapper objectMapper() {
    return JsonMapper.builder().findAndAddModules().build();
  }
}
