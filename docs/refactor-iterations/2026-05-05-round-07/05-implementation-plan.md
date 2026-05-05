# Implementation Plan

## Scope For This Round

Implement the reliability foundation:

1. Add notification outbox persistence.
2. Enqueue a pending notification when a match is detected.
3. Dispatch pending notifications without failing ingestion.
4. Store attempt count, last error, last attempt timestamp, sent timestamp, and Telegram message id.
5. Surface pending/failed notification counts on the dashboard.
6. Update tests and run the Maven suite.

## Steps

1. Create `notification` persistence types:
   - `NotificationOutboxEntity`
   - `NotificationOutboxRepository`
   - `NotificationDeliveryStatus`
   - `NotificationDispatchResult`
2. Change `TelegramNotifier.send` to return a delivery receipt with `messageId`.
3. Refactor `NotificationService`:
   - `enqueueMatchNotification(TrackedMatchEntity)`
   - `dispatchPendingForPlayer(PlayerEntity)`
   - attempt recording and retry scheduling.
4. Refactor `PollingService`:
   - remove direct send/mark-sent coupling from match detection,
   - enqueue after new match creation,
   - dispatch pending before/after player ingestion,
   - count sent notifications from dispatch results only.
5. Keep `TrackedMatchService.markNotificationSent` as compatibility projection.
6. Add dashboard metrics for pending and failed notifications.
7. Update unit and integration tests.

## Completed In This Round

- Added `notification_outbox` persistence with status, attempts, last error, last attempt, next attempt, sent timestamp, and Telegram message id.
- Added a transactional delivery recorder so outbox sent state and `TrackedMatchEntity.notificationSent` projection are updated together.
- Changed polling to enqueue detected matches and dispatch pending notifications without failing player sync when Telegram delivery fails.
- Added a compatibility bridge from legacy `tracked_matches.notification_sent=false` rows into the outbox.
- Ensured the in-memory poll lock is released even when `PollRunService.startRun()` fails.
- Changed Telegram delivery to return `TelegramDeliveryReceipt`.
- Added explicit HTTP timeout and transient retry properties for Riot and Telegram.
- Restricted actuator exposure and disabled OSIV in main/test properties.
- Added pending/failed notification counts to the dashboard.
- Removed long service-level transactions around player Riot PUUID resolution.
- Updated unit and integration coverage.

## Out Of Scope But Prioritized Next

- Persistent poll lock and active run status.
- Riot 429 `Retry-After` pause model.
- RestClient migration for Riot/Telegram.
- Flyway/Liquibase migrations and explicit indexes.
- Global `Match` + `PlayerMatch`.
- Full UI player detail/edit/archive/audit workflows.

## Validation Plan

- Unit tests for notification enqueue/dispatch success/failure.
- Polling tests proving Telegram failure does not fail player sync.
- Integration flow test with mocked Telegram receipt.
- Dashboard test for new outbox metrics.
- Full `mvn test`.
