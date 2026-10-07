# Issue #11: Actualizar Maven Wrapper y plugins de build

Issue #11 is sufficiently defined for the authorized local implementation phase. The previous 404 comes from an extra `maven/` segment in the distribution URL: it should use `https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.10.13/apache-maven-3.10.13-bin.zip`. The checked-in wrapper scripts are 3.3.6 and support SHA-256 verification. Keep the work to the wrapper, build plugins, and build documentation; do not change application dependencies or service code. Verification and any PR remain contingent on the prescribed wrapper build succeeding.

## Steps

- Correct the Maven 3.10.13 distribution URL and add its verified SHA-256 checksum to `.mvn/wrapper/maven-wrapper.properties`.
- Review the effective Maven plugin versions from the project POM and Spring Boot parent; update only build plugin versions that need maintenance, keeping application dependency versions unchanged.
- Document reproducible wrapper updates and Java/Maven compatibility in build documentation.
- Run `./mvnw.cmd -B -ntp verify` with Java 17 from the checkout and record the result; do not open a PR unless verification is green.

## Acceptance

- The wrapper downloads stable Maven 3.x from a valid URL and validates its SHA-256 checksum.
- Effectively used build plugins have suitable maintained versions explicitly pinned where appropriate.
- Build documentation explains reproducible wrapper updates and compatibility.
- Wrapper verification succeeds with Java 17 without requiring globally installed Maven; changes stay within wrapper, build plugins, and build documentation.
