# Validation evidence — 2026-10-11

Production is **not deployed**. Target miniPC/Cloudflare access was not identified
from the available Windows desktop. No pending PR was merged or deployed.

## Merged application

- Source: master `c81bf83169c1a8103c417ec38f22533a54c11b22`.
- Public GHCR index: `sha256:2bb21686c733aca6cee42592c80aa745fce85bbae7e28c8932b794ea316c02b4`, linux/amd64.
- Actions run 38089481339: test-and-publish success; complete workflow waiting for
  self-hosted deploy job. Not accepted by the new updater.
- Java 17.0.20.1, Maven wrapper: `mvnw.cmd -B -ntp verify` passed 228 tests,
  0 failures/errors/skips before the change.
- Despite passing tests, the actual GHCR image failed on an isolated fresh
  PostgreSQL 15 database: Hibernate reports missing `app_configuration` table.
- Regression test changed to rely on automatic Flyway migration, not manual
  migration inside DynamicPropertySource: reproduced the missing-table failure.
- Added Spring Boot Flyway starter. Full same Maven command then passed all 228
  tests with no skips, including fresh PostgreSQL startup. Candidate Java code
  was tested, not published/deployed as a production image.
- Final credential review also found raw Telegram exceptions could propagate a
  token-bearing URL. The adapter now returns sanitized errors, retaining only
  HTTP status and validated Retry-After metadata. Regression tests cover HTTP
  error bodies/headers and nested transport causes without contacting Telegram.
- Final `mvnw.cmd -B -ntp verify` after both fixes: **230 tests, zero failures,
  errors or skips**, Java 17 with Docker/PostgreSQL available.

## Deployment toolkit

- 12 Python policy/transaction tests passed: reject PR/fork/failed/pending/wrong
  workflow, reject latest/wrong SHA/run/attempt, mutually exclusive lock,
  migration review gate, crash marker, schema-incompatible rollback refusal,
  simulated failed-health rollback and successful-state recording after health.
  These passed on both Windows and Linux.
- `docker compose config --quiet` succeeded.
- Separate project `lol-tracker-validation`, with secrets in an owner-restricted
  directory outside Git. PostgreSQL and proxy became healthy. Application was
  stopped after identifying the merged-image startup failure.
- Demonstration GET / returned 200 with no-store. GET administration, actuator
  and API routes returned 404. POST polling/integration/root, PUT and DELETE
  player routes returned 403, all with no-store.
- PostgreSQL had no published ports. Proxy only bound 127.0.0.1:18085; configured
  application administration only binds 127.0.0.1:18086.
- Inserted a marker in the new validation database only. pg_dump completed;
  pg_restore successfully restored it into a newly created isolated database.
  PostgreSQL restart preserved the marker.
- Existing Plex, Prowlarr, qBittorrent, Radarr and Sonarr were not modified.
- All three validation containers were stopped after the checks; the new test
  volume, owner-protected local secrets and backup were preserved. All five
  existing multimedia HTTP services returned 200 after the checks.

## Not verified / activation requirements

- Linux systemd units, actual target architecture and recovery after full host reboot.
- End-to-end new Actions → artifact → local updater; workflow must first be
  merged by the owner, then succeed, and a read-only GitHub token must be entered.
- Target DNS, Cloudflare ingress, external HTTPS/cache and other public websites.
- Public live consultation UI and separate remote administration authorization.
- Riot Production authorization, real Riot credentials and Telegram destination.
- Off-device encrypted backup destination and real-data recovery exercise.

These limits must remain visible in any deployment handoff. Unit-test simulations
do not establish production rollback or delivery-chain success.
