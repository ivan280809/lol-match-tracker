# Riot API P2 Roadmap

## Scope

This roadmap covers API-backed V2 improvements that can improve the product without changing the round-11 runtime fix. It uses `docs/reference/riot-lol-api.md`, Riot's official API reference pages, Riot's League of Legends support/reference article, and the current round-11 decisions.

The roadmap includes:

- Data Dragon and static metadata.
- LoL status diagnostics.
- Spectator live game state.
- Champion mastery.
- Match timeline.
- Challenges.

This document does not authorize Java changes in round 11. Each item below needs its own follow-up iteration decision before implementation.

## Source Findings

- Data Dragon provides versioned static game data and assets for champions, items, summoner spells, profile icons, languages, and related metadata. Riot's League of Legends support/reference article says Data Dragon can lag a live game patch because Riot updates it manually.
- Static Riot files provide queues, maps, seasons, game modes, and game types. The app already has a local queue catalog, so Data Dragon/static work should start as enrichment and validation, not as a forced replacement.
- `lol-status-v4` exposes platform status at `/lol/status/v4/platform-data`. It belongs in ops diagnostics because it can explain Riot-side outages before operators blame the API key, polling, or Telegram.
- `spectator-v5` exposes current-game data at `/lol/spectator/v5/active-games/by-summoner/{encryptedPUUID}`. Riot labels the operation as PUUID-based despite the path name.
- `champion-mastery-v4` exposes all mastery entries, top mastery entries, one champion's mastery entry, and total mastery score by PUUID.
- `match-v5` already drives polling with match IDs and match detail. The timeline endpoint adds match-level event/frame data at `/lol/match/v5/matches/{matchId}/timeline` and can double match traffic if fetched for every stored match.
- `lol-challenges-v1` exposes challenge configuration, percentiles, player progress, and leaderboards. Config/percentile data is cacheable; player progress can be expensive across many tracked players.
- Riot rate limits apply at application, method, and service levels, and Riot requires clients to respect `Retry-After` on `429`.
- Riot policy forbids products from exposing game-session-specific information that gives players an unfair advantage. Spectator UI must stay conservative unless a later policy review approves a richer surface.

## Priority Summary

| Priority | Feature | Decision |
| --- | --- | --- |
| P2.1 | Data Dragon/static metadata | Implement first. It improves current UI copy/assets and has the lowest Riot API risk. |
| P2.2 | LoL status diagnostics | Implement second. It strengthens ops without touching polling or notification flow. |
| P2.3 | Spectator live game state | Implement third, manual/TTL-bound only. Keep the UI narrow and policy-safe. |
| P2.4 | Champion mastery | Implement fourth as profile enrichment with stale-tolerant caching. |
| P2.5 | Match timeline | Implement fifth, on-demand only for stored matches. Persist reduced insights, not raw timeline JSON. |
| P2.6 | Challenges | Implement last. It needs season-aware UX, larger payload handling, and careful prioritization. |

## P2.1 Data Dragon And Static Metadata

### Product Value

- Show champion names/icons in match lists, player detail, mastery cards, and notifications.
- Replace hard-coded or incomplete queue labels with official static queue metadata.
- Expose the active static-data version in ops so operators know when labels/assets may be stale.

### Suggested Ports And Services

- Add `LeagueStaticMetadataPort`.
  - `fetchLatestDataDragonVersion()`.
  - `fetchChampionCatalog(version, locale)`.
  - `fetchSummonerSpellCatalog(version, locale)`.
  - `fetchItemCatalog(version, locale)`, only when the UI needs item assets.
  - `fetchQueueCatalog()`, `fetchMapCatalog()`, `fetchGameModeCatalog()`, `fetchGameTypeCatalog()`.
- Add `LeagueStaticMetadataService` as the application boundary.
  - It owns refresh decisions, stale fallback, and model mapping.
  - It should depend on the port, not on `RiotClient` HTTP details.
- Keep versioned metadata separate from match persistence. Match rows should keep reduced match facts and reference static IDs such as `championId` and `queueId`.

### Cache And TTL

- Data Dragon version list: refresh every 12 to 24 hours and expose a manual ops refresh.
- Versioned Data Dragon payloads: cache indefinitely by `version + locale + type`; fetch a new copy only after the version changes.
- Static constants: refresh every 7 days or by manual ops action. They change rarely.
- Assets: prefer CDN URLs derived from version and key. Do not proxy or store image bytes unless offline operation becomes a requirement.

### UI Surface

- Dashboard config/ops: static-data version, last refresh time, stale/error state, manual refresh action.
- Player detail: champion icons and localized champion names in recent matches.
- Match list filters: official queue labels with fallback to the current local label.
- Notifications: champion names can use metadata when available, but missing metadata must not block delivery.

### Tests

- Adapter tests with fixture JSON for `versions.json`, `champion.json`, summoner spells, items, and static queue files.
- Service tests for version change, stale fallback, locale fallback, and malformed payload handling.
- MVC tests for metadata-present and metadata-missing rendering.
- Integration tests if metadata gets persisted: migration, unique key by `version + locale + type`, and no duplicate rows after refresh.
- End-to-end test with mocked static metadata port. No Riot or CDN real calls.

### Rate-Limit And Runtime Risks

- Data Dragon CDN calls do not carry the same Riot API-key rate-limit profile, but they still add network dependency.
- Large metadata payloads can slow startup if fetched eagerly. Refresh only after startup or by manual action.
- Riot may update the live game before Data Dragon publishes matching static data. UI must tolerate unknown champion or queue IDs.

## P2.2 LoL Status Diagnostics

### Product Value

- Show Riot platform incidents and maintenance in the ops dashboard.
- Add context to failed account validation, rank refresh, match polling, and Spectator checks.
- Reduce support guesswork when Riot is degraded.

### Suggested Ports And Services

- Add `RiotStatusPort`.
  - `fetchPlatformStatus(RiotPlatform platform)`.
- Add `RiotStatusDiagnosticsService`.
  - It maps Riot's platform-data response into a small view model: platform, fetched time, incident count, maintenance count, headline summaries, and failure category.
  - It should record external call logs with integration `RIOT` and operation `STATUS`.
- Keep `RiotOperationsPort` focused on key validation. Status deserves a separate port because it has different caching and UI behavior.

### Cache And TTL

- Positive status response: 5 minutes per platform.
- Error response: 1 minute negative cache, with the error category visible in ops.
- Manual refresh: allowed from the dashboard, still subject to a small local cooldown such as 30 seconds.

### UI Surface

- Dashboard health panel: Riot platform status, last checked time, incident/maintenance count, and the first one or two active headlines.
- Audit page: include recent status check failures beside existing Riot call logs.
- Player detail: no player-level status panel. Keep the signal at ops level.

### Tests

- Adapter test for `/lol/status/v4/platform-data` path, platform host, token header, `429` behavior, and malformed body.
- Service tests for TTL, negative cache, incident mapping, stale fallback, and log recording.
- Dashboard MVC tests for healthy, degraded, and unknown status.
- End-to-end dashboard test with mocked `RiotStatusPort`.

### Rate-Limit And Runtime Risks

- Repeated dashboard refreshes can create avoidable platform calls. Cache before rendering.
- Status is platform-routed. Use the selected player/platform context or a global configured platform; do not infer it from regional Match-V5 config.
- Status failures must not flip the whole app health to failed when DB, config, and Telegram are healthy. Treat it as a Riot diagnostic signal.

## P2.3 Spectator Live Game State

### Product Value

- Show whether a tracked player is currently in a live game.
- Help operators understand why recent match polling has not yet produced a result.
- Optionally support a future "in game now" player filter.

### Suggested Ports And Services

- Add `RiotSpectatorPort`.
  - `findActiveGame(RiotPlatform platform, String puuid)` returns an optional `LiveGameSnapshot`.
  - Map Riot `404` to "not in game" and cache it briefly.
- Add `LiveGameStateService`.
  - It owns per-player TTL, refresh triggers, and policy-safe projection.
  - It should expose only the fields the UI needs: player, queue ID, game start time, elapsed time, platform, and tracked-player champion if available.
- Do not add Spectator to the global poll loop by default. Start with manual or lazy dashboard refresh.

### Cache And TTL

- Active game found: 60 seconds per `platform + puuid`.
- No active game: 60 to 120 seconds negative cache per `platform + puuid`.
- Riot error or rate limit: 2 to 5 minute backoff using existing `Retry-After` handling when available.
- Persisting live state is optional. If persisted, expire rows after 15 minutes without refresh.

### UI Surface

- Player list: small "In game" or "Idle" state only when the cache is fresh.
- Player detail: live game panel with queue label, elapsed time, and last checked time.
- Ops dashboard: manual refresh action for one player or a bounded batch.
- Avoid showing enemy/team compositions, bans, or strategic live details in the first release. A later policy review can decide whether more detail is acceptable for private-only deployments.

### Tests

- Adapter tests for active response, `404` no-game mapping, platform host, token header, and `429` retry/backoff.
- Service tests for positive cache, negative cache, manual refresh cooldown, stale state expiry, and "not configured" behavior.
- MVC tests for no cache, active game, no game, and stale/error states.
- End-to-end test with mocked Spectator port and no real Riot calls.

### Rate-Limit And Runtime Risks

- A batch refresh across all tracked players can burn method limits quickly. Keep batch size configurable and off the scheduled poll path at first.
- Negative checks are still Riot calls. Cache "not in game" responses.
- Live-game data has policy risk. Keep public-facing output conservative.
- Spectator data can disappear when the game ends. The UI must not treat disappearance as an error.

## P2.4 Champion Mastery

### Product Value

- Add top champion mastery cards to player detail pages.
- Give operators/player viewers useful profile context without affecting polling or notifications.
- Pair naturally with Data Dragon champion icons and names.

### Suggested Ports And Services

- Add `RiotChampionMasteryPort`.
  - `fetchTopMasteries(RiotPlatform platform, String puuid, int count)`.
  - `fetchMasteryScore(RiotPlatform platform, String puuid)`.
  - Defer all-masteries and single-champion lookup until a real UI needs them.
- Add `PlayerMasteryService`.
  - It owns refresh policy, stale fallback, and profile view composition.
  - It should combine mastery data with `LeagueStaticMetadataService` for champion labels.
- Persist reduced mastery snapshots if the UI should survive restart or support stale display.

### Cache And TTL

- Top masteries: 12 hours per player.
- Total mastery score: 12 hours per player.
- Manual refresh: allowed on player detail with 5 minute cooldown.
- Store `last_refreshed_at`, `last_success_at`, and last error category if persisted.

### UI Surface

- Player detail: top 3 to 5 champions with icon, champion name, level, points, and last play time if available.
- Dashboard player table: optional compact top champion icon after Data Dragon exists.
- Notifications: no mastery content in P2. Keep match notifications focused.

### Tests

- Adapter tests for top mastery and score endpoints.
- Service tests for TTL, stale fallback, player missing/inactive behavior, Data Dragon join fallback, and refresh cooldown.
- Persistence integration tests for reduced snapshot upsert if persisted.
- MVC tests for mastery present, missing, stale, and error states.

### Rate-Limit And Runtime Risks

- Refreshing all tracked players at once can create a per-platform burst. Use manual refresh first, then a small scheduled refresh budget later.
- All-mastery responses can be larger than top-N responses. Use top endpoint first.
- Mastery changes slowly relative to match polling. Long TTLs are acceptable.

## P2.5 Match Timeline

### Product Value

- Add richer post-game analysis for stored matches: objectives, early deaths, gold swings, or key event markers.
- Improve match detail pages without changing dedupe, notification identity, or core match storage.

### Suggested Ports And Services

- Add `RiotMatchTimelinePort`.
  - `fetchTimeline(String matchId)`.
- Keep it separate from `RiotMatchPort` initially so the polling path cannot fetch timelines by accident.
- Add `MatchTimelineInsightService`.
  - It converts raw timeline frames/events into a reduced `MatchTimelineInsight`.
  - It persists only the small facts the UI needs. Do not store raw timeline JSON as the default.
- Add match-detail UI only after the reduced model has a clear product use.

### Cache And TTL

- Successful timeline fetch: immutable cache by `matchId`. A completed match timeline should not need periodic refresh.
- Missing timeline or transient failure: retry manually or after 24 hours; do not retry on every page render.
- In-memory cache can help tests, but persisted reduced insights are better once the UI depends on them.

### UI Surface

- Player match detail: objective/event timeline and small post-game notes.
- Dashboard recent matches: no timeline fetch. Link to detail only.
- Notifications: no timeline enrichment in P2 because it can delay delivery and double API traffic.

### Tests

- Adapter test for `/lol/match/v5/matches/{matchId}/timeline`, regional host, token header, and `429` behavior.
- Parser tests with timeline fixtures for classic games, remakes/short games, missing optional fields, and empty event arrays.
- Service tests proving one timeline fetch per match, immutable cache, and no calls from the polling notification path.
- Persistence integration test for reduced insight upsert if persisted.
- MVC/end-to-end test for match detail rendering with mocked timeline insight service.

### Rate-Limit And Runtime Risks

- Timeline can double Match-V5 traffic if fetched for every new match. Keep it on-demand.
- Payload size and schema breadth are higher than match summary. Reduce before persistence.
- Old or unusual game modes can produce sparse frames/events. The parser must tolerate missing data.

## P2.6 Challenges

### Product Value

- Add achievement-style player profile context after core match/rank/profile surfaces are mature.
- Show selected challenge progress or top challenge categories without changing notifications.

### Suggested Ports And Services

- Add `RiotChallengesPort`.
  - `fetchChallengeConfig(RiotPlatform platform)`.
  - `fetchChallengePercentiles(RiotPlatform platform)`.
  - `fetchPlayerChallengeData(RiotPlatform platform, String puuid)`.
  - Defer leaderboards; they are not needed for the tracker.
- Add `PlayerChallengesService`.
  - It owns season-aware mapping, cache policy, and reduced view selection.
  - It should choose a small set of displayed challenge groups instead of dumping the full Riot payload into UI.

### Cache And TTL

- Challenge config: 7 days per platform.
- Challenge percentiles: 24 hours per platform.
- Player challenge data: 12 to 24 hours per player.
- Manual refresh: player detail action with 10 minute cooldown.
- Include season/version fields in cache keys where Riot responses require it.

### UI Surface

- Player detail: small "Challenges" section with top categories, current level, percentile when available, and last refreshed time.
- Dashboard: no global challenges table in P2.
- Notifications: no challenge content in P2.

### Tests

- Adapter tests for config, percentiles, and player data paths.
- Service tests for season-aware cache keys, stale fallback, reduced selection, missing translation handling, and cooldown.
- Persistence integration tests if reduced challenge snapshots are stored.
- MVC tests for no data, partial data, stale data, and error state.
- End-to-end player detail test with mocked challenge port.

### Rate-Limit And Runtime Risks

- Config and percentile payloads are broad. Cache them before any player view tries to join data.
- Player challenge data across many players can create refresh storms. Use manual refresh first.
- Challenge UX can become noisy. Limit P2 to a small read-only profile surface.
- Leaderboards add high product complexity and little value for match tracking. Keep them out of P2.

## Cross-Cutting Implementation Order

1. Implement `LeagueStaticMetadataPort` and `LeagueStaticMetadataService`.
2. Add static metadata storage or cache, Data Dragon version refresh, queue/champion labels, and ops visibility.
3. Implement `RiotStatusPort` and dashboard diagnostics with TTL and external call logs.
4. Implement `RiotSpectatorPort` behind manual player refresh and conservative UI.
5. Implement `RiotChampionMasteryPort` and player-detail mastery cards.
6. Implement `RiotMatchTimelinePort` with on-demand reduced insight persistence.
7. Implement `RiotChallengesPort` after profile enrichment patterns and cache infrastructure are proven.

## Cross-Cutting Test Standard

Each follow-up iteration should include:

- Unit tests for service cache/TTL behavior and stale fallback.
- Adapter tests using local mocked HTTP responses. No real Riot, Data Dragon, or CDN calls.
- Persistence integration tests for any cache or snapshot table.
- MVC tests for the server-rendered UI states: present, missing, stale, and error.
- End-to-end tests with mocked ports covering the new user/ops flow.
- A local full build/test run before marking the iteration complete.

## Recommendations

- Start with static metadata because it improves existing screens and supports later mastery/timeline/challenges UI.
- Add LoL status diagnostics before live-player or profile enrichment. Operators need to distinguish Riot outages from tracker bugs.
- Keep live and profile features lazy/manual until the team has measured call volume with the current tracked roster size.
- Persist reduced snapshots only when the UI depends on stale display after restart. Do not persist raw Riot payloads by default.
- Add one port per new Riot API family. Avoid expanding `RiotClient` responsibilities without a matching interface and service boundary.
- Keep notification delivery independent of every P2 feature. Missing metadata, mastery, status, Spectator, timeline, or challenge data must not block Telegram messages.

## Risks

- Rate-limit pressure can grow faster than feature value if the app refreshes every tracked player on every dashboard render.
- Spectator data can create policy risk if the UI exposes game-session-specific details that players would not otherwise know.
- Timeline and challenges payloads can push the app toward raw JSON storage unless each iteration defines a reduced read model first.
- Data Dragon version drift can produce unknown champion/queue labels after a live patch.
- Adding many UI panels at once can make the simple server-rendered dashboard harder to scan.

## Handoff Items

- `architecture-analyst`: decide whether static metadata cache is DB-backed in the first implementation or starts in memory with manual refresh.
- `performance-analyst`: set per-feature call budgets based on tracked player count, configured platform count, and development/personal key limits.
- `cohesion-analyst`: confirm each new port belongs in `integration.riot` or merits a broader `integration.staticdata` package.
- `domain-refactor-agent`: define reduced domain/view models before any adapter returns raw Riot DTOs to UI services.
- `persistence-refactor-agent`: design cache/snapshot tables only for data that needs stale display after restart.
- `integration-refactor-agent`: implement one API family at a time with `Retry-After` support and explicit operation names for metrics/logs.
- `ui-ops-agent`: keep new dashboard surfaces compact, server-rendered, and clear about freshness.
- `quality-agent`: add mocked HTTP adapter tests, service TTL tests, MVC tests, and end-to-end flows without real Riot calls.
- `review-agent`: check that no P2 feature enters the polling or notification hot path without an explicit iteration decision.

## Unresolved Risks

- The exact persisted metadata schema is undecided.
- The first Spectator release needs a policy review if the deployment will serve users outside a private operator context.
- The product has not chosen which timeline insights matter enough to persist.
- The product has not chosen which challenge categories belong on player detail.
- Development/personal API-key limits may force smaller refresh budgets than the UI team wants.
