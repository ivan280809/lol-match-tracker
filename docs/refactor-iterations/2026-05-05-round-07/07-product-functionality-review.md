# Product And Functionality Review

## Implemented Product Improvements

- Telegram delivery is now visible as separate pending and failed notification counts on the dashboard.
- Telegram failures no longer make a successful Riot ingest look like a failed player sync.
- Poll run status colors now treat backend `ERROR` as a danger state.
- Actuator exposure is reduced to operationally useful endpoints only.

## High-Value Next Improvements

- Dashboard active polling state: show running duration, current player, and disable/manual-run feedback while a run is active.
- Notification outbox audit view: pending rows, failed rows, last error, last attempt, next attempt, and manual retry.
- Riot health panel: configured state, last successful request, last 429, last auth failure, and validation button.
- Telegram health panel: configured state, last successful send, failed count, and test-send button.
- Player detail page: match history, rank, recent form, champion stats, last syncs, and last errors.
- Edit/archive player flows: preserve history while removing noisy inactive players from the main roster.
- Dashboard filters/sorting: active/error/platform/result/champion/date and sort by last error, sync, rank, name, activity.
- Backfill controls when adding a player: import history without notification and set `track_from`.
- Runtime polling controls: enabled/manual-only, fixed delay, match window size, and safe page limit.

## Technical Backlog With Product Impact

- Riot 429 handling with `Retry-After` and UI pause messaging.
- Global `Match` + `PlayerMatch` model for shared-match cards and deduplicated Riot match fetches.
- Reduced match data expansion: KDA, role/lane, CS, gold, damage, vision, queue id, champion id.
- Richer Telegram cards using the expanded reduced stats.
- Config source visibility: DB vs env fallback without leaking secrets.
- Secret rotation/versioning for `APP_CONFIG_ENCRYPTION_KEY`.
- Flyway/Liquibase migrations and explicit PostgreSQL indexes.
- Testcontainers PostgreSQL and browser E2E tests.
- Metrics for poll duration, Riot calls/errors, Telegram sends/failures, notification backlog, and rate limits.
- Data retention for poll runs and old match history.

## Explicit Non-Goals For Round 07

- No global match schema migration.
- No persistent poll lock.
- No full Riot error taxonomy or rate-limit pause model.
- No browser automation suite.
- No runtime UI for polling configuration.
