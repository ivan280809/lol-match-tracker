# Performance Analysis

## Scope

This analysis focuses on polling cost, external HTTP behavior, database access, and operational growth risks.

## Findings

- Riot match id lookup is fixed at `count=10`, so active players can lose matches between polls.
- Full match payloads are fetched once per player/match pair. If several tracked players share a match, the same Riot match can be requested repeatedly.
- HTTP calls have no visible connect/read timeout policy at the adapter level.
- There is no retry/backoff distinction between transient 5xx/timeouts and functional 4xx responses.
- Riot 429 is not classified or paused using `Retry-After`.
- Pending notification retries are currently read per player from `tracked_matches.notification_sent=false`.
- There are no explicit indexes for pending notifications, player/game time history, champion history, or poll runs.
- Actuator exposes `*`, which increases operational noise and surface area.
- `spring.jpa.open-in-view` is not disabled, so view rendering can hide lazy loading.

## Recommendations

- Start with outbox indexes: status/next attempt, tracked match uniqueness, and created time.
- Add explicit adapter timeouts and a small retry policy for Telegram and Riot transient failures.
- Add Riot error taxonomy before implementing pause behavior, so UI and poll logic have stable categories.
- Move to configurable match window plus paginated Match-V5 search in a later round.
- Add a global Riot match cache when the model moves to global `Match` + `PlayerMatch`.
- Restrict actuator to `health,info,metrics` now; add Prometheus only when consumed.
- Disable OSIV once dashboard queries are checked for lazy access.

## Risks

- Retrying too aggressively can worsen Riot rate-limit behavior.
- Adding paging before dedupe/caching can increase API consumption.
- Testcontainers and Flyway are valuable but should be introduced with schema ownership, not mixed into a reliability patch without time to validate.

## Handoff Items

- Implement minimal notification outbox queries and keep dispatch batch bounded.
- Add property-driven HTTP timeout/retry values.
- Add docs backlog items for Riot paging, cache, DB migrations, indexes, metrics, and retention.

## Subagent Coordination Notes

`performance-analyst` highlighted the highest remaining risks: no Riot 429 pause model, no persistent poll lock, per-match dedupe queries, repeated rank refresh during notification stats, missing migrations/indexes, and no retention. This round implements timeout and retry properties for transient HTTP failures only. Riot 429 is intentionally not retried and remains a next-round classified error/pause feature.
