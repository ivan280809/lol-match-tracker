# Cohesion Analysis

## Scope

Maintain modular monolith boundaries while enriching Telegram output.

## Findings

- Stats calculation belongs in notification application logic because it is product copy and notification context.
- Queue classification remains in the match module through `MatchQueueCatalog`.
- Rank selection remains in the player module through `PlayerRankService`.

## Recommendation

Introduce small notification records for performance profiles and deltas. Keep formatting in the factory and calculations in the stats service.

## Risks

- Large records can become awkward. Keep them focused on Telegram needs.

## Handoff

- If these stats become useful in UI too, move them to a shared read-model service later.
