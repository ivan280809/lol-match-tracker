# Round 01 Implementation Plan

## Goal

Turn the approved design and decisions into executable changes with isolated subagent responsibilities.

## Tasks

- `architecture-analyst`: document the current architectural problems and target module boundaries. Completed.
- `performance-analyst`: document concrete inefficiencies in polling, network access, persistence, and object mapping. Completed.
- `cohesion-analyst`: document duplicated models, misplaced responsibilities, and simplification targets. Completed.
- Consolidate findings into the decisions document.
- `domain-refactor-agent`: define the target package layout, application services, use cases, and scheduler flow. In progress, initial implementation complete.
- `persistence-refactor-agent`: introduce managed players, reduced tracked matches, and poll run persistence. Initial implementation complete.
- `integration-refactor-agent`: replace internal HTTP collaboration and clean Riot and Telegram adapters. Initial implementation complete.
- `ui-ops-agent`: implement the server-rendered management and operations UI. Initial implementation complete.
- `quality-agent`: add unit, integration, end-to-end, and UI validation. Initial implementation complete.
- `review-agent`: review the integrated result and define any follow-up round. Pending.

## Sequencing

1. Finish analysis artifacts.
2. Lock decisions.
3. Refactor domain and persistence foundations.
4. Refactor external integrations.
5. Implement operational UI and endpoints.
6. Build validation coverage.
7. Run review and either accept or open round 02.

## Dependencies

- Domain refactor depends on approved architectural decisions.
- Persistence refactor depends on domain model decisions.
- UI depends on the managed player model and operational read models.
- Quality work depends on stable endpoints and service boundaries.

## Acceptance Criteria

- Players can be managed from UI and API.
- Polling reads active players from persistence.
- New matches are deduplicated and stored in reduced form.
- Telegram notifications are generated for newly detected matches.
- Internal HTTP calls inside the monolith are removed.
- Automated validation suite passes.

## Current Status

- New modular application path implemented under `com.loltracker.app`.
- Spring Boot scanning moved to the new modular path.
- Legacy code and obsolete internal modules were removed from the repository.
- The old static player source file was removed from runtime resources.
- Cleanup and final review are still pending.
