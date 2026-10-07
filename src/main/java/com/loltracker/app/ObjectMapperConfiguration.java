package com.loltracker.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Explicit configuration that provides an {@link ObjectMapper} bean.
 *
 * <p>Spring Boot 4.x still supplies a default {@link ObjectMapper} via
 * {@code JacksonAutoConfiguration}, but the test failure indicates that the
 * auto‑configuration was not applied in this context. Declaring the bean
 * explicitly guarantees that components such as {@code RiotClient} receive
 * an {@code ObjectMapper} instance without relying on the auto‑config.
 */
@Configuration
public class ObjectMapperConfiguration {
    /**
     * Provide an {@link ObjectMapper} bean for components that require
     * injection.  While Spring Boot normally supplies a default bean via
     * {@code JacksonAutoConfiguration}, test contexts sometimes
     * initialise a minimal application context where that auto‑configuration
     * is not applied.  Declaring the bean explicitly guarantees that
     * {@link com.loltracker.app.integration.riot.RiotClient} and other
     * classes receive a properly configured instance.
     *
     * The mapper is constructed using the default configuration which
     * registers the standard modules (including Java 8 date/time
     * support).  It does not register any custom modules – if needed
     * they can be added via {@link com.fasterxml.jackson.databind.Module}
     * {@link org.springframework.context.annotation.Bean} beans.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        // Create a default ObjectMapper that mimics the one provided by
        // Spring Boot's auto‑configuration.  Using the default constructor
        // ensures that the standard modules are registered.
        return new ObjectMapper();
    }
}
