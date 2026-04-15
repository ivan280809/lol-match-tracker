# Round 01 Cohesion Analysis

## Scope

Analyze responsibility placement, duplicated models, accidental complexity, and simplification opportunities.

## Findings

- The codebase contains multiple parallel DTO and model trees for very similar Riot data, split across player and match history modules.
- Controllers, use cases, services, and adapters do not have a consistently enforced responsibility boundary.
- Notification flow is fragmented between message building, notification forwarding, controller handling, and Telegram sending, despite all of it living in one application.
- Polling orchestration is hidden inside a player service layer rather than expressed as an operational application capability.
- Current naming still reflects a microservice mindset (`player service`, `match history service`, `notification service`) instead of cohesive internal modules.
- The system stores and transports objects shaped around external payloads more than around the actual business problem.

## Recommendations

- Collapse duplicated external payload models to the minimum needed for integration and mapping.
- Reframe the project around internal use cases: manage players, run polls, store tracked matches, send notifications, inspect operations.
- Centralize scheduling and orchestration logic in dedicated application services.
- Keep controllers thin and keep persistence concerns behind repositories and ports.
- Rename packages and concepts to reflect domain responsibilities rather than historical pseudo-service boundaries.

## Risks

- Renaming and collapsing models will create large diffs and may obscure semantic regressions without strong tests.
- Some current code may appear reusable but will become cheaper to replace than to preserve.

## Handoff

- `domain-refactor-agent`: reshape internal use cases and naming.
- `ui-ops-agent`: expose operations around the new cohesive model.
- `review-agent`: verify the refactor does not reintroduce service-shaped fragmentation.
