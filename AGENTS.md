# Project Agents

## Purpose

This repository hosts a single Spring Boot application that:
- manages a list of League of Legends players,
- polls Riot match history periodically,
- stores a reduced match history,
- sends Telegram notifications for new results.

The project must evolve as a cohesive monolith. Internal HTTP calls between modules are not allowed in the target architecture.

## Working Mode

- Treat the codebase as a modular monolith.
- Favor clear application services, ports, adapters, and persistence boundaries.
- Prefer Spring MVC + JPA consistency over mixed reactive/blocking flows unless there is a strong reason otherwise.
- Keep reliability basic: simple retries, clear logs, idempotent match tracking.
- Keep Telegram configuration global.
- Keep polling globally configurable.
- Keep security out of scope for now.

## Source Of Truth

- Architectural design lives in `docs/plans/`.
- Each refactor iteration lives in `docs/refactor-iterations/`.
- Persistent subagent definitions live in `project-agents/`.
- Implementation must follow the latest approved design and latest decision log.

## Mandatory Artifacts Per Iteration

Each iteration folder must contain:
- `01-architecture-analysis.md`
- `02-performance-analysis.md`
- `03-cohesion-analysis.md`
- `04-decisions.md`
- `05-implementation-plan.md`
- `06-validation-report.md`

Do not start implementation for an iteration until the decision document exists.

## Subagent Model

Analysis subagents:
- `architecture-analyst`
- `performance-analyst`
- `cohesion-analyst`

Implementation subagents:
- `domain-refactor-agent`
- `persistence-refactor-agent`
- `integration-refactor-agent`
- `ui-ops-agent`
- `quality-agent`
- `review-agent`

## Subagent Contracts

Each subagent must produce:
- scope,
- findings,
- recommendations,
- risks,
- explicit handoff items for the next subagent.

Implementation subagents must also produce:
- changed files,
- tests added or updated,
- unresolved risks.

## Architectural Directives

- Remove self-calls over HTTP inside the monolith.
- Replace `players.json` as the operational source with a managed data source.
- Persist only reduced match data needed for deduplication, UI, and notifications.
- Make polling a first-class application capability, not startup side effects hidden in random services.
- Keep external integrations behind explicit ports.
- Keep UI server-rendered and simple.
- Keep configuration explicit and environment-driven.

## Validation Standard

A refactor is not complete unless it includes:
- unit tests,
- integration tests,
- end-to-end tests without Riot real calls,
- UI tests,
- successful local build and test execution.

## Documentation Rules

- Record major decisions in iteration documents, not only in commit messages.
- Prefer concise markdown with actionable content.
- When assumptions are made, state them explicitly.
- When a decision rejects an alternative, write the reason.

