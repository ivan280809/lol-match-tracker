package com.loltracker.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
