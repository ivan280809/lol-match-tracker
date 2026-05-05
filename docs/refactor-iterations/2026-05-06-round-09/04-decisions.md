# Decisions

## Decision 1: Use Blocking `RestClient` Adapters

Riot and Telegram adapters will use Spring `RestClient`, not `WebClient` followed by `.block()`.

Reason: the application is Spring MVC + JPA. Blocking adapters make thread and transaction behavior explicit and remove unnecessary reactive dependencies.

Rejected alternative: keep WebClient with `.block()`. It keeps the current mixed model and hides retry/error behavior behind reactive plumbing.

## Decision 2: Pause The Poll Run On Riot 429

When Riot returns 429, the app reads `Retry-After` and pauses the whole poll run until that instant. If the header is absent or invalid, it uses a conservative fallback.

Reason: Riot rate limits apply broadly enough that immediately continuing with other players can worsen pressure. A global pause is predictable and easy to explain in the UI.

Rejected alternative: pause only the current player. It keeps the run moving but can still hammer Match-V5 for other players during a global/app limit.

## Decision 3: Persist A DB Poll Lock With Lease

Polling will acquire a persisted lock row before starting. The lock has owner, acquired time, and lease expiry.

Reason: this prevents concurrent runs across multiple instances while allowing recovery if an instance dies mid-run.

Rejected alternative: keep only `AtomicBoolean`. It protects one JVM only.

## Decision 4: Persist Polling Configuration With Env Fallback

Operators can configure polling enabled/disabled, manual-only, fixed delay, match window size, and pagination page limit from the UI. When no DB config exists, environment properties remain the fallback.

Reason: V2 operations need runtime control without redeploying.

Rejected alternative: keep polling as env-only. It is too rigid for a distributable app.

## Decision 5: Default New Players To `track_from=created_at`

New players will only notify matches ending after their `track_from` timestamp. The default is creation time. An explicit backfill mode imports older matched history with notifications suppressed.

Reason: adding an existing player should not spam Telegram with old games.

Rejected alternative: always notify every newly discovered Riot match. That is surprising and noisy.

## Decision 6: Introduce Global Match Model By Dual-Write

Round 09 adds `matches` and `player_matches` and writes them during polling, while keeping `tracked_matches` and the current outbox/UI reads intact.

Reason: a full read-model/outbox migration is too large for one safe iteration. Dual-write gives shared-match dedupe and future migration data without breaking current product flows.

Rejected alternative: switch all reads and outbox to `player_matches` now. It touches dashboard, detail pages, notification stats, outbox uniqueness, and audits in one broad change.

## Decision 7: PUUID Is Functional Identity When Available

The service rejects duplicate resolved PUUIDs and treats `gameName#tagLine` as mutable display data. The existing `game_name/tag_line` unique constraint stays for compatibility until a safe migration can remove or relax it.

Reason: Riot ID can change while PUUID remains stable.

Rejected alternative: continue treating `gameName#tagLine` as the only identity. It breaks when players rename.

## Decision 8: Use Injectable Clock And Configurable Zone

Services that produce timestamps or local-day calculations will use injected `Clock` and `ZoneId`. `Europe/Madrid` remains only the default configuration value.

Reason: tests become deterministic and deployments can choose their operating timezone.

Rejected alternative: keep hard-coded `Instant.now()` and `Europe/Madrid` throughout services. It makes behavior harder to test and less distributable.

## Decision 9: Multi-Region Is Not Fully Supported Yet

Round 09 keeps one global Riot regional routing cluster for Account-V1 and Match-V5, plus per-player platform routing for League platform APIs.

Reason: the current product model and UI are built around one Riot regional cluster. True multi-region history polling would require per-player regional routing decisions and migration rules.

Rejected alternative: introduce full multi-region now. It widens identity, polling, and dedupe semantics beyond this iteration.
