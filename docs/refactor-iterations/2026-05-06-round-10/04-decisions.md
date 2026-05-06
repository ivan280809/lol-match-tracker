# Decisions

## Decision 1: Adopt Flyway For Production Schema Ownership

Production will use Flyway migrations and `spring.jpa.hibernate.ddl-auto=validate`.

Reason: `ddl-auto=update` is not a distributable release strategy.

Rejected alternative: keep Hibernate schema update. It can drift across installations and does not provide reviewed SQL.

## Decision 2: Backfill Global Match Tables From Legacy `tracked_matches`

Round 10 will add an idempotent migration that copies existing reduced match history into `matches` and `player_matches`.

Reason: Round 09 dual-writes only new data. Existing installs need history preserved in the target model.

Rejected alternative: leave legacy history behind. That would make shared stats incomplete after upgrade.

## Decision 3: Keep Outbox Compatibility For This Iteration

Dashboard/stat read paths can move toward `player_matches`, but `notification_outbox` remains attached to `tracked_matches`.

Reason: notification delivery and retry behavior is already reliable. Moving the outbox FK during the same migration would widen release risk.

Rejected alternative: migrate notification outbox to `player_matches` now. It requires careful data migration and retry semantics not needed to unblock V2 distribution.

## Decision 4: Use Optional Minimal Access Guard

Add a small, disabled-by-default dashboard guard for deployments exposed outside trusted LAN.

Reason: security remains mostly out of scope, but a distributable app should not make operators patch this themselves.

Rejected alternative: full Spring Security user management. It is excessive for this project phase.

## Decision 5: Testcontainers And Browser Tests Must Be Honest

Add PostgreSQL/Testcontainers and browser-style tests where feasible, but make environment-dependent tests skip-safe and record actual execution.

Reason: local environments may lack Docker/browser tooling. A skipped capability is acceptable only if reported clearly.

Rejected alternative: pretend MVC tests are browser tests. That would hide the validation gap.

## Decision 6: Metrics Must Stay Low Cardinality

Counters/timers will use operation/status/category labels only, not player, PUUID, or match ids.

Reason: high-cardinality metrics can damage production observability.

Rejected alternative: tag metrics by player or match for debugging convenience.

## Decision 7: Shared Telegram Aggregation Is Opt-In Preparation

Round 10 can add the service capability and tests, but default notification behavior remains compatible unless the change is fully validated.

Reason: avoiding duplicate/spam is already solved. Aggregated message formatting can be enabled after read-model migration is stable.

Rejected alternative: switch defaults immediately. It risks surprising users who expect per-player notifications.
