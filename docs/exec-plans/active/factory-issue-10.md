# Issue #10: Actualizar Spring Boot y dependencias Java gestionadas por el proyecto

Repair the Spring Boot 4.1.1 migration's missing Jackson ObjectMapper bean in the local writer phase. The startup failure points to RiotClient injection; the dependency entry for jackson-databind alone does not supply Spring Boot's Jackson auto-configuration. Preserve the approved dependency migration and MVC/JPA design.

## Steps

- Inspect src/main/java/com/loltracker/app/config and the application/test configuration for existing Jackson or MVC auto-configuration exclusions; use the provided file map and relevant source only.
- Restore Boot-managed Jackson auto-configuration for the application's MVC stack, preferably by relying on spring-boot-starter-web's JSON starter and removing redundant direct jackson-databind declaration if it is not otherwise justified. If source configuration excludes that auto-configuration, remove or correct the exclusion rather than constructing an unmanaged ObjectMapper.
- Inspect the RiotClient and related context-loading tests, including src/test/resources/application.properties and any test configuration, to confirm the bean is available in production and tests without adding behavior-specific test workarounds.
- Run ./mvnw.cmd -B -ntp verify on Java 17, including the full suite and PostgreSQL tests where Docker is available; address only migration-related compatibility failures within the approved issue.
- Document relevant source and destination dependency versions, any incompatible API changes, and verification outcomes in project documentation. Leave Maven Wrapper and Maven plugins unchanged.

## Acceptance

- Spring application contexts start with a Boot-configured com.fasterxml.jackson.databind.ObjectMapper injectable into RiotClient.
- The existing Riot client JSON parsing and MVC/JPA behavior remain intact; no reactive stack is introduced.
- Spring Boot-managed dependency versions are retained unless an explicit compatibility justification is documented; no preview releases are used.
- ./mvnw.cmd -B -ntp verify passes on Java 17, with PostgreSQL test coverage reported accurately if Docker is unavailable.
- Documentation records relevant dependency versions and any required migration changes; Maven Wrapper and Maven plugins remain unchanged.

## Operator diagnosis 2026-10-09

Read src/main/java/com/loltracker/app/ObjectMapperConfiguration.java, which currently contains only comments, and the ObjectMapper injection types in RiotClient and TelegramNotifier. Fresh PersistenceIntegrationTest fails because no com.fasterxml.jackson.databind.ObjectMapper bean exists. Add a real @Configuration in package com.loltracker.app (within component scan) providing an explicitly named @Bean of that exact Jackson 2 type, using com.fasterxml.jackson.databind.json.JsonMapper.builder().findAndAddModules().build() or an equivalent module-aware Jackson 2 mapper. Keep the Boot Jackson 3 mapper distinct; do not remove this compatibility bean on the assumption Boot creates a Jackson 2 bean, and do not pass tools.jackson instances to com.fasterxml constructors. Add a focused context/serialization regression if needed. Preserve current BOM-managed upgrade and never touch unreadable AppConfigurationService.java. Inspect new verification failures and fix the actual next cause; do not downgrade dependencies or disable tests to hide it.
