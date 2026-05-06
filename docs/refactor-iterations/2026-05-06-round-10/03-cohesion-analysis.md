# Cohesion Analysis

## Scope

This document assigns Round 10 responsibilities across the monolith.

## Findings

- Flyway belongs at the application infrastructure boundary, but migration contents reflect entities from `player`, `match`, `notification`, `ops`, and `settings`.
- Match/global read logic belongs in `match`, not controllers.
- Optional access guard belongs in `ops` or `config`, with no broad security model beyond a small operational boundary.
- Metrics should be emitted from services that own the action being measured.
- Browser/MVC E2E tests should exercise product flows through the public UI/API, not internals.

## Recommendations

- Keep migration SQL in `src/main/resources/db/migration`.
- Add read services/repositories in `match` for shared stats and queue labels.
- Keep outbox write compatibility until notification aggregation is migrated safely.
- Add optional access control through properties:
  - disabled by default,
  - simple username/password or token,
  - actuator health remains accessible.
- Keep observability labels low-cardinality.

## Risks

- Security dependencies can widen the app surface if overbuilt.
- Shared notification aggregation can couple `notification` too deeply to match participants; keep it optional and compact.
- Concurrent subagent edits can conflict around `pom.xml`, application properties, and dashboard templates.

## Handoff Items

- Subagents must use disjoint write scopes where possible.
- The coordinator owns shared files: `pom.xml`, application properties, and final validation docs.
