# Validation Report

## Status

Implemented and locally validated.

## Commands

- `.\mvnw.cmd "-Dtest=PlayerRankServiceTest,NotificationStatsServiceTest,NotificationMessageFactoryTest" test`
- `.\mvnw.cmd test`
- `.\mvnw.cmd -DskipTests package`

## Results

- Focused tests: success. 17 tests, 0 failures, 0 errors, 0 skipped.
- Full suite: success. 203 tests, 0 failures, 0 errors, 2 skipped. The skipped tests are the existing PostgreSQL/Testcontainers tests because Docker is not available locally.
- Package: success. Built `target\lol-match-tracker-1.0.0.jar`.

## Notes

- The first full test run exposed a cleanup issue when Hibernate generated the test schema without DB-level cascade. `PlayerRankEntity` now declares `@OnDelete(CASCADE)` to match the Flyway migration and keep test/runtime schema behavior aligned.
- Maven still prints existing Java 25 warnings for restricted/native access and Mockito dynamic agent loading. They are non-blocking in this iteration.
