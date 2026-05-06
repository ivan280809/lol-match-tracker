# Decisions

## Decision 1: Enrich Telegram With Persisted Data First

Round 12 will add KDA, KDA ratio, CS, CS/min, gold, gold/min, damage, damage/min, vision, vision/min, lane/role, queue label/id, platform/region, and match id to Telegram using existing persisted fields.

Reason: these fields are already stored and do not increase Riot traffic.

Rejected alternative: call Riot Match-V5 timeline, Data Dragon, or other APIs during Telegram dispatch. That would add rate-limit and latency risk to a core notification path.

## Decision 2: Surface Shared Match Context Without Changing Outbox Semantics

If multiple tracked players are present in the same match, the existing individual notification will include a compact "tracked players together" section.

Reason: it adds immediate value while preserving the stable per-player outbox model.

Rejected alternative: switch to aggregated shared Telegram cards in this iteration. That requires new outbox identity and duplicate-prevention rules.

## Decision 3: Keep Telegram HTML Compact And ASCII-Safe

The message will use Telegram HTML tags, short section headers, and stable labels without emoji or unsupported formatting.

Reason: Telegram HTML has a limited supported subset and mobile readability matters.

Rejected alternative: use emoji-heavy or MarkdownV2 formatting. That increases escaping and rendering risk.

## Decision 4: Document API-Backed "Everything Else" As Follow-Up

Data Dragon icons/assets, Riot status, Spectator, mastery, timeline insights, challenges, RSO, replays, and imports remain planned behind explicit ports, caches, TTLs, and tests.

Reason: "add everything" is technically correct only when the app can protect polling and notification reliability.

Rejected alternative: implement every API family now. It would widen scope beyond a safe distributable iteration.
