# Decisions

## Decision 1: Implement Notification Outbox First

The first implementation slice will add a simple persisted outbox for match notifications.

Reason: it directly fixes the highest reliability risk: Telegram outages must not block match ingestion.

Rejected alternative: keep using `tracked_matches.notification_sent=false` as the retry queue. It cannot store attempt count, last error, last attempt timestamp, next retry, or Telegram message id.

## Decision 2: Keep Existing Match Table In This Round

The round will not migrate to global `Match` + `PlayerMatch`.

Reason: that model is correct for shared-match stats, but it is a larger schema and service refactor. The outbox can be introduced safely on top of the current `TrackedMatchEntity`.

Rejected alternative: combine outbox and global match migration. This would make validation much harder and increase regression risk in the dashboard.

## Decision 3: Notification Failures Do Not Mark Player Sync Failed

Polling will record Riot/player failures separately from notification delivery failures. A Telegram failure records an outbox attempt and leaves the player sync result based on Riot ingestion.

Reason: the user value is preserving match history even when Telegram is down.

Rejected alternative: report Telegram failures as player errors. This hides the real integration boundary and makes a successful Riot sync look failed.

## Decision 4: Store Telegram Delivery Receipt When Available

The Telegram adapter will parse `message_id` from successful responses and store it on the outbox row.

Reason: this improves auditability and reduces duplicate uncertainty after transient failures.

Rejected alternative: only store `notification_sent_at`. It cannot correlate app state with Telegram delivery.

## Decision 5: Use Explicit Adapter Timeouts And Basic Retry Next

HTTP reliability will be handled at integration-adapter boundaries with explicit timeout values and transient retry behavior.

Reason: Spring MVC/JPA consistency improves when external calls are plain blocking adapter calls with clear failure mapping.

Rejected alternative: continue using scattered `WebClient.block()` calls with default timeout behavior.

## Decision 6: Record Remaining Product Backlog After Implementation

The final pass will document product/functionality improvements not implemented in this round.

Reason: the supplied list is larger than one safe iteration. Recording the backlog keeps the project moving without pretending every item was completed.
