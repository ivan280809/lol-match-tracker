# Cohesion Analysis

## Scope

Check whether rank reliability and Telegram improvements stay aligned with the modular monolith direction.

## Findings

- Riot access is already behind ports. The rank fix belongs in the Riot adapter, not in notification code.
- Notification message formatting is isolated in `NotificationMessageFactory`.
- Rank selection and persistence are isolated in `PlayerRankService`.
- The UI already has ops surfaces for integration health and player sync errors; rank status can reuse the same language.

## Recommendations

- Keep `RiotRankPort` as the boundary and avoid leaking HTTP endpoint details into notification services.
- Add small domain/value semantics for rank availability instead of passing raw exception text to Telegram.
- Update tests at the boundary level: `RiotClientTest`, `PlayerRankServiceTest`, `NotificationStatsServiceTest`, and `NotificationMessageFactoryTest`.
- Keep P2/P3 ideas in documentation unless they fit existing ports without schema churn.

## Risks

- Adding too many fields to notification snapshots can turn a formatter record into a mixed domain model.
- Introducing live-game or mastery features now would need new ports and UI flows, widening the iteration.

## Handoff

- P1 work should use concise product copy.
- P2/P3 work should update docs and not create unused runtime paths.
