# Issue #9: Actualizar el proyecto local a Java 26

Proceed with the authorized Java 26 upgrade against the verified baseline, keeping Spring Boot 4.1.1 and repairing the Maven plugin resolution failure by selecting a released compiler plugin version available from Maven Central. The failed run did not reach compilation or tests, so it is not evidence of a source incompatibility.

## Steps

- Inspect the current Maven configuration and align Java compilation settings to 26; prefer the `release` setting for the target and avoid introducing unrelated dependency changes.
- Replace the unavailable `maven-compiler-plugin` 3.18.1 pin with a released version resolvable from Maven Central that supports JDK 26. Preserve Lombok annotation processing and verify its configured version supports JDK 26.
- Check the Maven Wrapper scripts and properties for Java-version requirements; keep wrapper distribution integrity settings intact and ensure the documented local runtime requirement is JDK 26.
- Review active build/runtime references across Dockerfile, both Compose files, GitHub Actions, README, CONTRIBUTING, and WORKFLOW. Align any Java-version references to 26; retain already-compatible Temurin 26 Docker and Actions configuration.
- Update setup documentation to state JDK 26 is required for local builds and that the project targets Java 26; remove stale Java 17 build instructions from active paths.
- Run `./mvnw.cmd -B -ntp verify` under the configured Temurin 26 runtime, with Docker available so PostgreSQL tests are not skipped. Record the test totals and Maven result.
- Build the Docker image through the project Dockerfile and validate the Compose configuration; report any environmental limitation separately from code failures.

## Acceptance

- Maven declares and compiles for Java 26 consistently, using a compiler plugin version that resolves from Maven Central and supports JDK 26.
- Lombok annotation processing works on JDK 26, and Spring Boot remains at 4.1.1.
- Active wrapper, Docker, Compose, GitHub Actions, and setup documentation paths consistently support Java 26, with no stale Java 17 build instructions.
- `./mvnw.cmd -B -ntp verify` succeeds under Temurin 26 with no skipped tests.
- Docker image build and Compose configuration validation succeed.
- Only files necessary for the Java 26 migration and documentation are changed; known limitations are documented.
