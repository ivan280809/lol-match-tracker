# Architecture Analysis

## Scope

Round 09 moves the monolith toward a V2 distributable shape without replacing the server-rendered product surface:

- Riot reliability and friendly error classification.
- Blocking HTTP adapters and explicit integration ports.
- Persistent polling lock, active run state, and operator polling configuration.
- Controlled onboarding semantics with `track_from` and backfill without Telegram spam.
- Match-V5 pagination, per-run match-detail cache, and global `Match` + `PlayerMatch` dual-write.
- Clock/timezone configuration and initial enum cleanup.

## Findings

- The current Riot and Telegram adapters use `WebClient` with `.block()`, which mixes reactive plumbing into a Spring MVC/JPA application.
- `PollingService` uses an in-memory `AtomicBoolean`; it prevents local overlap only, not overlap across multiple app instances.
- Poll run records know start/finish/status, but not the current player, current step, active pause, or rate-limit state.
- Polling behavior is environment-only. There is no persisted operator control for enabled/disabled, manual-only mode, match window size, or pagination safety.
- Player creation resolves PUUID when Riot is configured, but new players can still receive old match notifications because there is no `track_from` boundary.
- Match storage is still per-player in `tracked_matches`; this prevents shared-match deduplication and forces repeated match-detail calls when tracked players share the same game.
- `TrackedMatchEntity` stores too little reduced Riot data for a serious V2: it lacks KDA, queue id, lane/role, CS, gold, damage, vision, platform, and region.
- Rank refresh happens from notification stats while a stats transaction is active, so an external Riot call can be inside transactional work.
- `Europe/Madrid` is hard-coded in notification formatting and stats grouping.

## Recommendations

- Replace reactive clients with `RestClient` plus small synchronous retry helpers. Retry timeouts/network/5xx with backoff, and never retry functional 4xx or immediate 429.
- Introduce ports for Riot account, match, rank, and Telegram notification behavior while keeping current adapters as implementations.
- Use a DB-backed poll lock with lease expiry and owner token. Keep the existing in-process guard as a cheap local fast path, but make the DB lock authoritative.
- Add poll run fields for active operator state: current player, current stage, active pause until, and rate-limit message.
- Store polling configuration with DB values overriding env fallback. Let scheduled polling tick frequently and decide whether a run is due from persisted delay/manual-only/enabled state.
- Add `track_from` and a backfill mode to players. Default new players to `track_from=now`; optional backfill imports older matches with notification suppressed.
- Add `matches` and `player_matches` as the target global model, and dual-write from polling while keeping `tracked_matches` for current UI/outbox compatibility.
- Keep PUUID as the functional identity when available. Reject duplicate PUUIDs in service logic now; add a safe DB unique migration later after duplicate cleanup.
- Inject `Clock` and app `ZoneId`. Keep `Europe/Madrid` only as configurable default.

## Risks

- Full dashboard/outbox migration from `tracked_matches` to `player_matches` is larger than one safe iteration. Dual-write lowers risk but leaves temporary duplication.
- JPA `ddl-auto=update` still does not provide versioned migrations or guaranteed index creation behavior across production databases.
- First creation of a DB poll lock row can race on very first startup across instances. The implementation should catch duplicate insert and retry under lock.
- Riot 429 can happen during match ids or match detail. A global poll pause is simpler and safer than per-player pause, but it may delay other players.
- Scheduled polling with a frequent tick depends on persisted last-run timing and the lock. This is operationally acceptable but should be documented.

## Handoff Items

- `architecture-analyst`: keep V2 work inside the monolith; no internal HTTP calls.
- `performance-analyst`: bound Match-V5 pagination and use per-run cache for match details.
- `cohesion-analyst`: keep external calls out of transactions; persistence services should save resolved data after adapters return.
- `domain-refactor-agent`: introduce enums, `track_from`, backfill state, and PUUID duplicate semantics.
- `persistence-refactor-agent`: add poll lock, poll config fields, global match tables, and indexes.
- `integration-refactor-agent`: replace WebClient blocking with RestClient and map Riot errors to product categories.
- `ui-ops-agent`: expose active run, rate-limit pause, and polling config without making the dashboard heavier.
- `quality-agent`: test 429/Retry-After, timeout, 401/403, 404, 5xx, malformed response, backfill suppression, dedupe, lock, and dashboard state.
