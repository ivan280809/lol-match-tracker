# Implementation Plan

## Scope

Implement a complete UI/ops improvement while preserving Spring MVC + Thymeleaf + JPA and the modular monolith boundaries.

## Steps

1. Persistence and domain:
   - add `archived_at` to `PlayerEntity`,
   - update active-player polling queries to exclude archived players,
   - add archive/restore operations,
   - expose rank and archive fields in player views.
2. Configuration and integration health:
   - expose DB/ENV/No configurado source labels in `AppConfigurationView`,
   - add external call log entity/repository/service,
   - add DB/Riot/Telegram health view models.
3. Riot and Telegram operations:
   - classify Riot errors into friendly categories,
   - add Riot key validation action,
   - add account lookup/validation action,
   - add Telegram test-send action and safe result recording.
4. Dashboard UI:
   - add GET query param filter/sort model,
   - add compact rank snapshot to player cards,
   - add edit/detail/archive/restore actions,
   - add health panel and configuration source display.
5. Player pages:
   - add `/players/{id}` detail page,
   - add `/players/{id}/edit` and POST update flow,
   - show basic data, rank, sync state, match history, recent form, champion stats, and outbox warnings.
6. Audit:
   - add `/audit` page with player errors, pending/failed notifications, poll runs, and external check summaries.
7. Tests:
   - unit tests for player archive/update/account validation and ops health/logging,
   - MVC tests for dashboard filters, detail, edit, archive/restore, health actions, Telegram test, Riot validation, and audit,
   - integration tests with mocked Riot/Telegram adapters.
8. Validation:
   - run `.\mvnw.cmd test`,
   - run `.\mvnw.cmd -DskipTests package`,
   - update `06-validation-report.md` with real output and backlog.

## Assumptions

- Roster and audit volumes are small enough for bounded in-memory read-model composition in this round.
- No browser/E2E infrastructure exists yet; if still true, document it as a validation gap.
- Riot key validation can use a safe platform status endpoint through the existing Riot adapter.
- External check logs store only summaries, never API keys, bot tokens, chat IDs, or full request URLs.
