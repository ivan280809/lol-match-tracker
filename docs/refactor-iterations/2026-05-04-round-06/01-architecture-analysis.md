# Architecture Analysis

## Scope

Telegram v2 rank statistics and user-facing MVC error handling.

## Findings

- Riot match/account calls currently use regional routing only.
- League rank data requires platform routing.
- Player persistence is the right home for a small current rank snapshot.
- Dashboard controllers can still surface unexpected exceptions through Whitelabel.

## Recommendations

- Extend `RiotPlatform` with a platform API base URL.
- Keep Riot rank response parsing in `RiotClient`.
- Add a dedicated rank snapshot service around player persistence and score calculation.
- Add a global MVC exception handler for HTML requests.

## Risks

- Riot rate limits can make live rank refresh fail during notification.
- Adding rank fields changes the player table shape, relying on the existing Hibernate update strategy.

## Handoff

Implement platform-scoped Riot rank calls, persisted snapshots, notification stats additions, and MVC error handling tests.
