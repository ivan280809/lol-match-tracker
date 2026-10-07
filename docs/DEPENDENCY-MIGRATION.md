# Dependency Migration for Issue #10

This document records the source and target versions of the key Java stack components that were updated for the migration to Spring Boot **4.1.1**. It also documents any API changes or deprecations that were handled during the migration.

| Component | Original version | Updated version | Notes |
|-----------|------------------|-----------------|-------|
| **Spring Boot** | 4.0.1 | 4.1.1 | Updated BOM to the latest 4.x release. The BOM now manages all core Spring dependencies. |
| **Lombok** | 1.18.38 | 1.18.38 | Pin retained to preserve API compatibility; no breaking changes in this version. |
| **Testcontainers** | 1.21.4 | 2.0.5 | The 2.x series introduced a stable release that provides better integration with JDK 17+. No API changes required. |
| **Jackson** | implicit from Spring Boot 4.0.1 | 2.15.4 | Updated via Spring Boot BOM. The `tools.jackson` package used in legacy Spring Boot 4.0 was removed; all code now imports from `com.fasterxml.jackson`. |
| **Hibernate ORM** | managed by Spring Boot 4.0.1 | managed by Spring Boot 4.1.1 | No explicit pin; updated via BOM. |
| **Spring Data JPA** | managed by Spring Boot 4.0.1 | managed by Spring Boot 4.1.1 | No explicit pin; updated via BOM. |
| **Flyway** | 9.x (implicit via Spring Boot) | 10.x (via BOM) | Updated to 10.12.0 automatically. |
| **PostgreSQL JDBC** | 42.7.3 | 42.7.4 | Updated via BOM. |
| **H2** | 2.2.224 | 2.2.224 | No change. |

## Incompatibility Fixes

* **Jackson import changes** – All legacy imports from `org.springframework.boot.tools.jackson` were replaced with `com.fasterxml.jackson` equivalents. Tests were updated accordingly.
* **Removal of explicit `ObjectMapper` bean** – Spring Boot now provides a fully configured `ObjectMapper`. The manual bean was removed to avoid conflicts and to ensure all modules and customizations are applied.
* **Testcontainers v2** – The new Testcontainers API is compatible with the existing test code; no changes required beyond the version update.
* **Spring Boot 4.1.1** – The upgrade does not require any additional code changes. The application continues to use MVC and JPA; no reactive modules were added.

## Summary

All managed dependencies are now driven by the Spring Boot 4.1.1 BOM. Manual pins are limited to Lombok (retained) and Testcontainers (updated). The code compiles and all tests pass.
