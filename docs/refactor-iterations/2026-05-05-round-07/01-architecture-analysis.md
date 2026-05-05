# Architecture Analysis

## Scope

Round 07 reviews the current Spring Boot monolith against the requested V2 reliability and product backlog:

- notification outbox and Telegram failure isolation,
- Riot and Telegram HTTP boundaries,
- polling lifecycle and run visibility,
- player and match persistence model,
- runtime configuration and operational UI.

## Current State

The application is already a single Spring MVC + JPA monolith. Controllers call application services directly. There are no internal HTTP calls between modules.

Main packages:

- `player`: tracked roster and Riot account/rank resolution.
- `match`: reduced match persistence per player.
- `tracking`: scheduled/manual polling.
- `notification`: Telegram message building and stats.
- `integration.riot` and `integration.telegram`: external HTTP adapters.
- `settings`: persisted runtime configuration and secret encryption.
- `ops`: dashboard, operations API, poll run records.

## Findings

- Polling detects matches, persists them, sends Telegram, and marks delivery in one loop. This couples ingest reliability to Telegram availability.
- `TrackedMatchEntity.notificationSent` is both delivery state and dedupe signal. It lacks attempts, last error, last attempt timestamp, and external Telegram message id.
- `NotificationStatsService` refreshes Riot rank inside a transactional stats-building path. `PlayerService.create`, `update`, and `ensurePuuid` also call Riot from transactional methods.
- `RiotClient` and `TelegramNotifier` expose reactive `WebClient.block()` directly in otherwise MVC/JPA code.
- `PollRunEntity` records finished runs but has no persistent active lock, current player, or rate-limit pause fields.
- Match persistence is per player. It is sufficient for current UI/notifications, but it duplicates shared matches and blocks richer shared-game product features.
- Runtime configuration is partly persisted and partly environment fallback. The UI masks secrets but does not show source, test buttons, or rotation status.

## Recommendations

- Introduce a notification outbox as the immediate reliability boundary: match detection creates a durable pending notification, and Telegram delivery is retried separately.
- Keep `TrackedMatchEntity` for this iteration, but move delivery attempts to a new `notification_outbox` table and use the match flag only as a compatibility projection.
- Make HTTP adapters blocking and explicit with timeouts/retry helpers. Removing WebFlux from the dependency tree can follow once all WebClient usage is gone.
- Split external lookup and persistence in player/rank flows so Riot calls happen before or outside write transactions.
- Extend poll run state in later rounds with persistent lock/current-player/rate-limit metadata.
- Plan a later `matches` + `player_matches` migration for shared match cards and roster stats.

## Risks

- Introducing outbox without Flyway still relies on `ddl-auto=update`; acceptable for this round, but schema migrations should become a near-term decision.
- Retrying Telegram after partial failures can duplicate messages when Telegram accepts a message and the app crashes before persisting the message id. Storing `telegram_message_id` reduces this but does not eliminate every crash window.
- Bigger data model changes should not be bundled with the outbox, or the refactor becomes too broad to validate confidently.

## Handoff Items

- Persistence: create outbox entity/repository with unique tracked-match notification.
- Integration: make Telegram sender return a delivery receipt and map HTTP failures.
- Domain/application: enqueue on detection, dispatch pending notifications without failing player sync.
- UI/ops: surface pending/failed notification counts and leave richer controls for the next round.

## Subagent Coordination Notes

`architecture-analyst` confirmed the monolith has no internal self-HTTP calls and that round 07 should consolidate outbox as the delivery source of truth. It also flagged an atomicity risk between `outbox=SENT` and `tracked_matches.notification_sent`; implementation now records both in `NotificationDeliveryRecorder` inside one transaction.

The next architecture handoff is explicit ports for Riot/Telegram adapters and a future `Match` + `PlayerMatch` migration.
