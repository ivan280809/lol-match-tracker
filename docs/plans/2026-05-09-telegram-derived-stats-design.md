# Telegram Derived Stats Design

## Goal

Improve Telegram notifications using data already persisted by the monolith. Avoid new Riot calls in the notification path.

## Approach

The message will keep its compact match card and add derived context:

- Recent player profile from the last stored matches.
- Queue-specific profile for the current queue.
- Champion-specific profile.
- Position-specific profile when lane/role exists.
- Current match deltas against recent averages.
- Compact highlights for unusually good or risky signals.
- Shared-match combined KDA when multiple tracked players played together.

## Deployment

Flyway remains the source of schema change. This iteration adds indexes for the extra history lookups and makes deploy compose explicitly enable Flyway so migrations run before Hibernate validation.

## Rejected Scope

No timeline, item build, summoner spells, runes, objective data, live-game state, or Data Dragon assets are included here because those require new persisted data or new API calls.
