# Round 01 Performance Analysis

## Scope

Analyze polling flow, Riot calls, persistence access patterns, payload handling, and avoidable overhead.

## Findings

- The current flow performs multiple object-mapping passes over large Riot payloads and persists much more data than needed.
- Internal HTTP calls inside the same application add avoidable serialization, deserialization, routing, and failure surfaces.
- Deduplication currently depends on per-match existence checks and then full match fetches, which is serviceable at small scale but should be tightened around reduced persistence and clearer query paths.
- The reactive polling flow introduces complexity while the persistence layer remains blocking, which adds mental and runtime overhead without delivering a coherent non-blocking pipeline.
- Startup-triggered polling and periodic polling are coupled, making performance behavior less explicit during application lifecycle and local testing.
- The current schema recreation setting (`ddl-auto=create`) destroys runtime state on restart, which can force unnecessary refetching and duplicate processing pressure over time.

## Recommendations

- Remove internal HTTP calls and replace them with direct in-process collaboration.
- Persist only reduced match records and query them by player and `matchId`.
- Make polling execution explicit and driven by a dedicated scheduler service.
- Keep Riot fetch strategy simple but make deduplication persistence-backed and testable.
- Replace destructive schema recreation with a stable persistence approach.

## Risks

- Premature optimization is unnecessary; the system should first remove obvious overhead and complexity.
- Overcomplicating rate limiting or batching in the first wave would conflict with the chosen simplicity target.

## Handoff

- `integration-refactor-agent`: simplify client interactions and retry behavior.
- `persistence-refactor-agent`: design efficient reduced tables and lookup paths.
- `quality-agent`: validate polling and deduplication behavior with non-live tests.
