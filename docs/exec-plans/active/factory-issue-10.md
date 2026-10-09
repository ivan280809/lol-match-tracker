# Issue #10: Actualizar Spring Boot y dependencias Java gestionadas por el proyecto

Repair the dependency migration documentation and remove the redundant Lombok version override. Preserve the approved Spring Boot 4.1.1 / Testcontainers 2.0.5 upgrade and the Jackson 2 compatibility bean. No owner decision is needed.

## Steps

- In pom.xml, remove the lombok.version property and the Lombok dependency version; remove the matching version from the Lombok annotationProcessorPaths entry so both use Spring Boot's dependency management.
- Keep the explicit Jackson 2 jackson-databind dependency and the named Jackson 2 compatibility bean. Update docs/DEPENDENCY-MIGRATION.md to distinguish com.fasterxml.jackson (Jackson 2) from tools.jackson (Boot's Jackson 3), describe why the compatibility bean remains, and report resolved versions for both.
- Replace estimated or contradictory dependency entries in docs/DEPENDENCY-MIGRATION.md with source and target versions resolved from the original and upgraded builds. Cover relevant used components, including Hibernate ORM, Spring Framework, Spring Data JPA, Flyway modules, PostgreSQL JDBC, Lombok, Testcontainers and H2; identify BOM-managed components and explicit overrides accurately.
- Do not modify the protected execution plan, AppConfigurationService.java, Maven Wrapper, or Maven plugin versions. Preserve MVC/JPA and the existing compatibility configuration.
- The supervisor runs ./mvnw.cmd -B -ntp verify. Review any failures and provide a focused repair for the actual cause without downgrading dependencies or disabling tests.

## Acceptance

- Lombok resolves through the Spring Boot BOM without a redundant property or dependency/processor pin, unless the writer finds and documents a concrete compatibility requirement.
- Migration documentation records verifiable original and target resolved versions, identifies BOM-managed versus explicitly versioned dependencies, and accurately describes both Jackson packages and the Jackson 2 bean.
- Spring MVC/JPA behavior remains intact; no preview releases or reactive stack are introduced.
- The supervisor's full verify result is recorded, with any unresolved failure reported by its actual cause.
