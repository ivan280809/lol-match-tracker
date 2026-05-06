# Riot API P3 Deferred Ideas

## Scope

This document records nice-to-have Riot API and product ideas for Round 11. It is not an implementation plan for this iteration. The goal is to keep deferred ideas visible while preserving the approved Round 11 focus:

- P0: fix rank lookup by PUUID.
- P1: make rank availability product-aware in Telegram/UI copy.
- P2: keep only low-risk API enrichments near implementation.
- P3: defer features that need new product direction, auth, ports, persistence, UI flows, or operational ownership.

Sources reviewed:

- Round 11 architecture, performance, cohesion, decisions, and implementation plan.
- `docs/reference/riot-lol-api.md`, last reviewed 2026-05-06.
- Riot API reference, Developer Portal guide, League of Legends developer guide, and Riot FAQ.

## P3 Classification Rule

An idea is P3 when it does not improve core match polling, deduplication, stored reduced match history, or Telegram result reliability in the current product shape. P3 items may be valuable later, but they need at least one of the following before implementation:

- a clear user workflow,
- a new explicit port and adapter boundary,
- a cache/TTL and rate-limit budget,
- persistence design,
- Riot production or RSO access,
- operator-visible configuration,
- UI acceptance criteria.

Promotion rule:

- Promote to P2 when the feature is useful but still optional, has a bounded workflow, and can be implemented behind ports without destabilizing polling or notifications.
- Promote to P1 only when the feature becomes necessary for correctness, reliability, compliance, operator diagnosis, or a committed user-facing release goal.

## Deferred API Ideas

| Idea | Possible value | Why P3 now | Prerequisites | Risks | Promotion trigger |
| --- | --- | --- | --- | --- | --- |
| Clash (`clash-v1`) | Show tracked players' Clash teams, upcoming Clash tournaments, and simple team context. | The app is a match tracker, not a Clash team calendar or social roster tool. Clash data does not improve current polling, dedupe, or notification correctness. | Add `RiotClashPort`; define platform routing; add TTL for tournament/team lookups; design team/tournament UI; decide whether Telegram should ever mention Clash context. | Seasonal/event data can be stale or empty; team membership changes; UI can become a second product surface; extra Riot calls compete with core polling. | P2 if users ask for "who is playing Clash this weekend" visibility. P1 only if the product explicitly becomes Clash-team tracking or notifications require Clash context. |
| Tournament APIs (`tournament-v5`, `tournament-stub-v5`) | Generate tournament codes, receive callbacks, inspect lobby events, and support organized custom-game workflows. | This is a different product class from passive match tracking. It requires organizer workflows, callback hosting, and access decisions outside the current monolith scope. | Approved production key with tournament access; public callback URL and validation model; `RiotTournamentPort`; persistence for providers/tournaments/codes/callbacks; admin UI; E2E tests with tournament stub. | Callback delivery and security constraints; domain/certificate restrictions; misuse could create custom-game operational burden; significantly wider support surface. | P2 if a small admin-only tournament-code prototype is requested and can use stubs. P1 only if tournament operation becomes a committed product requirement. |
| RSO and authenticated match APIs (`account-v1`/`summoner-v4` `/me`, `lol-rso-match-v1`) | Let users link Riot accounts, confirm ownership, and access authenticated match data including custom matches. | Current app is a trusted monolith with manually managed players and no user accounts. RSO introduces OAuth, consent, token storage, callback routes, and privacy obligations. | Production application and RSO client; user/session model; secure token storage and refresh rules; consent copy; account-linking UI; `RiotAuthenticatedAccountPort`; privacy/ToS review. | Token leakage; unclear personal-data retention boundaries; multi-user auth adds security scope currently out of project scope; RSO access approval is external. | P2 if account ownership verification becomes useful for private deployments. P1 only if public/multi-user usage or Riot policy requires player opt-in for planned features. |
| Replays (`match-v5` replays and local Replay API) | Link or download replay files, or drive local replay camera/recording workflows. | Result notifications and dashboard history do not need replay files. Local Replay API depends on a user's installed League client and native/local execution, which does not fit the server monolith. | Define whether replay support means server-side replay file links or local desktop tooling; storage/retention policy; UI affordance; no-Riot-call test fixtures; local-client boundary if Replay API is used. | Large files and retention pressure; local API is only available on the player's machine; brittle UX when replays expire or are unavailable; privacy concerns for sharing. | P2 if the UI needs "open replay" links for recent matches without storing files. P1 only if replay evidence becomes core to a committed review/coaching feature. |
| Free champion rotation (`champion-v3`) | Show current free-to-play champions or newbie-friendly notices. | Low relevance to tracked-player match results. It is static-ish enrichment that does not affect polling, Telegram, or reduced match persistence. | Add cached `ChampionRotationPort`; Data Dragon champion mapping; long TTL; compact dashboard placement; tests for empty/failed rotation lookup. | Adds visual noise; endpoint value varies for experienced players; champion mapping can drift across patches if Data Dragon refresh is absent. | P2 if the dashboard gains a general LoL information panel. P1 is unlikely unless onboarding/free-rotation visibility becomes a named product goal. |
| League-Exp imports (`league-exp-v4`) | Import/backfill players from ranked ladders by queue, tier, and division. | Current source of truth is managed players, not ranked-ladder discovery. Imports can add many players and increase polling, notification, and rate-limit load. | Explicit import workflow; dry-run/preview UI; dedupe by PUUID/Riot ID; per-import rate-limit budget; opt-in tracking defaults; audit log; rollback/archive strategy. | Accidentally tracking too many players; noisy Telegram results; duplicates across regions/queues; user confusion over why players appeared. | P2 if operators need controlled bulk import for a known tier/division. P1 only if the product pivots to ladder monitoring or managed-player entry becomes a blocking operational problem. |

## Low-Priority UI Ideas

These UI ideas remain P3 because they are presentation improvements or exploratory surfaces. They should not create new Riot calls unless their backing data is already cached or persisted.

| UI idea | Why P3 | Prerequisites | Risks | Promotion trigger |
| --- | --- | --- | --- | --- |
| Clash card on player detail | Useful only when Clash data is available and requested. | Cached Clash team/tournament data and clear empty state. | Player detail becomes crowded; stale team data can mislead. | P2 when Clash lookup is approved as a bounded enrichment. |
| Tournament admin page | Belongs to tournament operations, not current passive tracking. | Tournament provider/code model, callback validation, stub E2E tests. | Large workflow surface and security concerns. | P2 for an internal prototype; P1 for real tournament hosting. |
| Account-linking settings page | Needs RSO and user identity, both out of scope today. | User/session model, RSO client, token storage, consent copy. | Turns a trusted admin app into a multi-user app. | P2 when ownership verification is requested; P1 when required for public use. |
| Replay action/link on match detail | Nice for review, not needed for result tracking. | Replay availability model and retention copy. | Broken links and expectations around expired replays. | P2 when recent-match replay links can be added without storing files. |
| Free rotation widget | Informational, not tied to tracked players. | Cached rotation plus champion assets. | Dashboard noise. | P2 when a broader LoL info/patch panel is planned. |
| Ranked ladder import wizard | Could reduce manual entry, but changes operational behavior. | Preview, dedupe, archive strategy, import audit. | Over-tracking and Telegram spam. | P2 when operators request bulk onboarding. |

## Findings

- Clash, tournament, RSO, replay, champion rotation, and League-Exp features are real Riot API surfaces, but none are necessary for Round 11 rank reliability.
- Tournament and RSO features are access- and policy-heavy compared with the current trusted-admin monolith.
- League-Exp imports are the only P3 item that could materially reduce admin effort, but they also create the highest risk of accidental scale and notification noise.
- Champion rotation is technically simple, but its product value is weak unless the dashboard becomes a broader League information surface.
- Replay support splits into two different products: server-visible Match-V5 replay links/files, and local client automation through the Replay API. They should not be blended casually.

## Recommendations

- Keep all P3 ideas out of Java implementation in Round 11.
- If one P3 item is promoted next, choose League-Exp imports only after designing import preview, dedupe, and opt-in tracking defaults.
- Treat RSO as a separate security/privacy iteration, not as a small Riot adapter change.
- Treat tournament support as a product pivot unless the first scope is a stub-only internal proof of concept.
- Prefer cached/static UI enrichment before live API UI panels.

## Risks

- Deferred ideas may look easy because endpoint lists are short, but the real cost is product workflow, consent, persistence, and operations.
- Adding live Riot calls for optional UI surfaces can reduce polling reliability under tight rate limits.
- RSO and tournament access depend on Riot approval and external policy constraints, so they cannot be guaranteed by code alone.
- Bulk imports can violate the current "managed player list" mental model if operators cannot preview and undo changes.

## Handoff Items

- Architecture agent: require a new port per promoted API family; do not expose Riot HTTP details to services or UI controllers.
- Performance agent: set TTLs and rate-limit budgets before any live P3 endpoint is used.
- Cohesion agent: confirm promoted work still supports the modular monolith and managed-data-source direction.
- UI/Ops agent: produce a workflow sketch and empty/error states before adding new UI panels.
- Quality agent: require unit, integration, E2E, and UI coverage without real Riot calls before marking any promoted P3 feature complete.
- Review agent: check that promoted P3 work does not create hidden startup side effects or internal HTTP calls.

## Changed Files

- `docs/refactor-iterations/2026-05-06-round-11/08-riot-api-p3-deferred.md`

## Tests Added Or Updated

- None. This is a documentation-only P3 artifact.

## Unresolved Risks

- No product owner has selected which deferred idea, if any, should move to P2.
- Riot approval requirements for RSO and tournament access remain external dependencies.
- The app still needs explicit workflow design before adding bulk imports, account linking, tournaments, or replay behavior.
