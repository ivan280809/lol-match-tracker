# Dependency Migration for Issue #10

This document records the source and target versions of the key Java stack components that were updated for the migration to Spring Boot **4.1.1**.  The following table lists each component, its original version in the source tree, the upgraded target version, and a brief note about the migration.

| Component | Original version | Updated version | Notes |
|-----------|------------------|-----------------|-------|
| **Spring Boot** | 4.0.1 | 4.1.1 | Spring Boot BOM now manages all core Spring dependencies.
| **Hibernate ORM** | 7.2.0.Final (managed by Spring Boot) | 7.4.5.Final | Updated via BOM; no explicit pin.
| **Spring Data JPA** | 4.0.1 (managed by Spring Boot) | 4.1.1 | Updated via BOM.
| **Flyway** | 11.14.1 (implicit via BOM) | 12.4.0 | Updated via BOM.
| **PostgreSQL JDBC** | 42.7.8 | 42.7.13 | Updated via BOM.
| **Jackson 2.x** | implicit via Spring Boot | 2.21.5 | Explicit `jackson-databind` dependency added; a dedicated `ObjectMapperConfiguration` supplies a module‑aware `ObjectMapper`.
| **Jackson 3.x** | implicit via Spring Boot | 3.1.5 | Managed by BOM; no code changes required.
| **Lombok** | 1.18.38 (explicit pin) | 1.18.46 | Explicit pin removed; BOM provides a compatible version.
| **Testcontainers** | 1.21.4 | 2.0.5 | 2.x series is fully compatible with the existing test code.
| **H2** | 2.4.240 | 2.4.240 | No change.

## Compatibility Evidence
The migration was verified by diffing the *source‑tree* (SHA `f92dc30f19ee27ca61966587ccd40c2a687d2b7a`) against the *target‑tree* (new commit after the upgrade).  The evidence is captured in:

* `docs/reports/issue-10-dependency-evidence/source-tree.txt`
* `docs/reports/issue-10-dependency-evidence/target-tree.txt`

These files list the exact Maven coordinates present in each tree, confirming that the target versions match the table above.

## Summary of Changes
* All core Spring dependencies are now controlled by the Spring Boot 4.1.1 BOM.
* Manual pins were limited to Lombok (now removed) and Testcontainers (updated to 2.0.5).
* A custom `ObjectMapperConfiguration` provides a Jackson 2 `ObjectMapper` that registers all modules on the classpath; the default Boot Jackson 3 mapper remains untouched.
* No API changes or deprecations were introduced by the migration.

The application continues to compile and all tests pass against the target commit.
