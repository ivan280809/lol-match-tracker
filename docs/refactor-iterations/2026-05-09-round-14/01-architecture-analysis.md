# Architecture Analysis

## Scope

Round 14 expands Telegram notifications with statistics derived from already stored matches and fixes deploy migration safety.

## Findings

- `tracked_matches` already stores enough data for KDA, CS/min, gold/min, damage/min, vision/min, lane/role, queue, result, duration, and shared tracked-player context.
- `player_rank_snapshots` was introduced in Round 13, but the deploy run failed because the production container validated the JPA model before the table existed.
- Notification enrichment can remain in `NotificationStatsService` and `NotificationMessageFactory` without crossing module boundaries or adding external calls.

## Recommendation

Keep the feature in the notification application service and use repository queries over `tracked_matches`. Add Flyway indexes for the query paths and explicitly force Flyway on in deployment.

## Risks

- Very dense Telegram messages can become noisy. Keep sections compact and avoid duplicating raw values already present above.
- Existing production env may have disabled Flyway; compose-level environment should override that.

## Handoff

- Persistence: add V4 indexes and deploy Flyway env.
- Domain/application: add derived stats records and calculations.
- Quality: update message and stats tests, then run full Maven validation.
