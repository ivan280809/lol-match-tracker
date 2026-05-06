# Architecture Analysis

## Scope

Round 12 improves Telegram notification content and presentation.

## Findings

- The app already persists reduced per-player match facts needed for a richer notification: KDA, CS, gold, damage, vision, queue, role/lane, duration, result, platform, and region.
- Shared tracked-player participation is available through the global `player_matches` read model.
- Adding Data Dragon, Spectator, timeline, mastery, or challenges directly to Telegram would add new external calls to the notification hot path.

## Recommendations

- Enrich Telegram using already persisted match and shared participation data.
- Keep the message compact and HTML-safe.
- Avoid new Riot/Data Dragon calls while dispatching Telegram notifications.
- Record API-backed enrichments as follow-up work behind caches and explicit ports.

## Risks

- A too-large Telegram message can become noisy on mobile.
- Shared participation may be unavailable for old or partially migrated data; the message must degrade gracefully.

## Handoff

- Implementation: update notification stats and formatting.
- Quality: cover formatting, escaping, derived rates, shared match copy, and no raw error text.
