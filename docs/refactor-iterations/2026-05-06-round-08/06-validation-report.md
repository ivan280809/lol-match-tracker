# Validation Report

## Status

Implementation completed with one validation gap: browser-driven E2E infrastructure is still absent.

## Commands

- `.\mvnw.cmd test`
- `.\mvnw.cmd -DskipTests package`
- `.\mvnw.cmd -Dtest=DashboardControllerTest test`
- `.\mvnw.cmd -Dtest=ExternalIntegrationsEndToEndTest test`
- `.\mvnw.cmd -Dspring-boot.run.useTestClasspath=true spring-boot:run` with H2 datasource environment overrides
- `Invoke-WebRequest -UseBasicParsing http://localhost:8080/`
- In-app browser DOM check through DevTools on `http://localhost:8080/`
- In-app browser functional check through DevTools on `http://localhost:8080/?sort=rank&tab=health`

## Results

- `.\mvnw.cmd test`: passed with 146 tests, 0 failures, 0 errors, 0 skipped.
- `.\mvnw.cmd -DskipTests package`: passed and produced `target/lol-match-tracker-1.0.0.jar`.
- `.\mvnw.cmd -Dtest=DashboardControllerTest test`: passed with 20 tests after compacting and functionally reviewing the dashboard layout.
- `.\mvnw.cmd -Dtest=ExternalIntegrationsEndToEndTest test`: passed with 69 tests, 0 failures, 0 errors, 0 skipped.
- Local review server: running on `http://localhost:8080/` with H2 in-memory datasource `jdbc:h2:mem:loltracker-local`; process PID 24152.
- `Invoke-WebRequest -UseBasicParsing http://localhost:8080/`: returned HTTP 200 with the local review server running.
- In-app browser DOM check: loaded `LOL Match Tracker`, confirmed compact filters/sidebar tabs, old deploy badge removed, and empty match feed hidden.
- In-app browser functional check: confirmed `sort=rank` shows `Orden: Rank`, changing the order selector auto-submits to `sort=activity`, updates the visible chip to `Orden: Actividad`, and preserves the `Salud` tab.

## Coverage Added Or Updated

- Unit tests:
  - player create/update/archive/restore and conditional Riot PUUID resolution,
  - roster filtering/sorting,
  - integration operation result recording for Riot and Telegram.
- MVC tests:
  - dashboard rendering and query-param filters,
  - dashboard filter labels, sort auto-submit contract, tab preservation, query-param normalization, and form draft preservation,
  - player detail,
  - player edit/update,
  - archive/restore actions,
  - Riot key validation,
  - account validation,
  - Telegram test action,
  - audit page.
- Integration tests:
  - UI/API/polling flow with mocked Riot and Telegram,
  - archived players are excluded from polling while retained,
  - Telegram test action records an external call log without real Telegram calls.
  - H2-backed server E2E suite with mocked Riot and Telegram ports,
  - full happy path through Riot key validation, Riot account validation, player create, polling, match persistence, rank refresh, Telegram notification, dashboard/detail/audit rendering,
  - Riot error categories covered for key validation, account validation, player create, player update, PUUID resolution during polling, recent match id fetch, match detail fetch, and rank refresh,
  - Telegram failure cases covered for manual test send and notification dispatch, including retryable failed outbox persistence.

## Notes

- Dashboard follow-up: the main page was compacted after visual review by moving advanced filters into a collapsible panel, grouping player/health/config forms into sidebar tabs, shrinking the header and metrics, and hiding the empty match feed when there are no matches.
- E2E follow-up: added `ExternalIntegrationsEndToEndTest`, which runs the Spring MVC/JPA stack against H2 and mocks only `RiotClient` and `TelegramNotifier`.
- E2E found and fixed two operational issues: rank snapshots could be overwritten by stale player sync saves, and newly enqueued notification outbox rows could occasionally miss immediate dispatch because of timestamp precision.
- Browser Use could not be used in this desktop session because its Node runtime was not available to the MCP server; a DevTools DOM check was used instead for the local browser sanity check.
- Browser-driven E2E tests were not added because the repository still has no browser automation infrastructure.
- Maven logs still include JDK/Maven warnings about restricted native access, deprecated `Unsafe`, Mockito dynamic agent attachment, and H2 dialect deprecation. They do not fail the build.
- New persistence remains compatible with current JPA `ddl-auto=update`; Flyway/Liquibase is still pending.

## Backlog

- Add browser E2E tests for dashboard filters, detail/edit/archive, health actions, audit navigation, and visual regressions.
- Add focused adapter classification tests for low-level WebClient HTTP status mapping if the Riot/Telegram adapters grow more behavior.
- Add Flyway/Liquibase migrations and explicit indexes for player archive/filter fields, match history filters, outbox status, and external call logs.
- Persist normal successful Riot/Telegram adapter calls, not only manual validation/test actions, if richer "last OK" semantics are needed.
- Add Riot 429 `Retry-After` pause state and UI messaging.
- Move large roster/history filtering to database specifications if data volume grows.
