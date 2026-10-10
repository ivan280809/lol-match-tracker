# Issue #9: Actualizar el proyecto local a Java 26

Implement the approved Java 26 upgrade on the current baseline, preserving Spring Boot 4.1.1 and application behavior. The repository excerpt identifies the relevant build, runtime, CI, and setup files; no additional owner decision is needed.

## Steps

- Update pom.xml to target Java 26 and ensure the compiler and Lombok annotation processing are compatible with JDK 26 while retaining Spring Boot 4.1.1.
- Update .github/workflows/publish-ghcr.yml to use a compatible JDK 26 distribution for build and verification.
- Update Dockerfile build and runtime images to compatible Temurin 26 images; keep the existing Docker Compose configuration unless inspection shows an active Java 17 reference there.
- Review .mvn/wrapper/maven-wrapper.properties and wrapper scripts; update only if the current wrapper itself prevents operation on JDK 26, keeping wrapper metadata aligned.
- Update README.md and active setup/build documentation to state JDK 26 requirements and remove Java 17 from active setup instructions. Update WORKFLOW.md and AGENTS.md only where they describe current build or verification requirements.
- Inspect relevant files for other active Java 17 references, including workflows and Compose configuration, and update only those needed for the issue. Do not change historical records or unrelated dependencies.
- Run ./mvnw -B -ntp verify under the configured Temurin 26 runtime, investigate any compatibility failures within this upgrade scope, and record the result and known limitations.
- Build the Docker image(s) used by the repository and record successful build evidence; do not deploy or publish.

## Acceptance

- Maven consistently targets Java 26 and the project compiles under the configured Temurin 26 runtime.
- Lombok and annotation processing work on JDK 26 with Spring Boot 4.1.1 retained.
- Active wrapper, Docker build/runtime, GitHub Actions, and setup/build documentation use Java versions compatible with JDK 26 and no longer prescribe Java 17.
- ./mvnw -B -ntp verify completes with the full suite and no skipped tests.
- Docker image build succeeds.
- Documentation states the local JDK requirement and any compatibility limitation discovered; no preview features or Java 27 are introduced.
