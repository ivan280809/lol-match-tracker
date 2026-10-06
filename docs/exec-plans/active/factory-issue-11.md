# Issue #11: Actualizar Maven Wrapper y plugins de build

The issue is well-scoped and the supplied project excerpts are sufficient for a local build-tooling update. Maven Wrapper 3.3.2 supports SHA-256 verification in both launcher scripts, but its distribution URL currently points to Maven 3.9.9 without a checksum. The project uses Spring Boot 4.0.1's parent plugin management and configures compiler, Surefire, and Spring Boot plugins without explicit versions. Proceed with a bounded update and document reproducible wrapper maintenance; preserve Java 17 and application dependencies.

## Steps

- Inspect the effective Maven plugin versions inherited from Spring Boot 4.0.1 and confirm the project’s Java and CI constraints.
- Update the wrapper distribution to a stable compatible Maven 3.x release, add its published SHA-256, and retain a wrapper version supported by local and CI environments.
- Pin stable maintained versions for the compiler, Surefire, and Spring Boot Maven plugins where appropriate, keeping build-tool changes separate from application dependency updates.
- Document the reproducible wrapper update and any relevant compatibility considerations.
- Run ./mvnw.cmd -B -ntp verify on Java 17 and report any environment-related skipped checks.

## Acceptance

- Wrapper distribution is stable Maven 3.x, configured for SHA-256 verification, and usable from a clean checkout without global Maven.
- Effectively used build plugins have reviewed stable versions and explicit versions where appropriate.
- Reproducible wrapper update instructions and compatibility notes are documented.
- No application dependency or service behavior changes are included.
- Build verification evidence is recorded, including any checks that cannot run in the environment.
