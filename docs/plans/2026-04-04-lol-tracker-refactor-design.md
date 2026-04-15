# LOL Tracker Refactor Design

Date: 2026-04-04
Status: Approved

## Objective

Refactor the application into a cohesive single Spring Boot application that:
- manages players through a simple server-rendered UI and API,
- polls Riot periodically using a global configurable interval,
- stores reduced match history,
- sends Telegram notifications for new match results,
- remains observable and testable without external live dependencies.

## Functional Scope

- Managed player source backed by a database.
- Simple server-rendered management UI.
- Global Telegram destination.
- Global polling configuration.
- Reduced persistence model for matches.
- Basic reliability with retries and logs.
- No authentication in the first version.

## Recommended Architecture

Use a modular monolith with four main internal domains:
- `player-management`
- `match-tracking`
- `notification`
- `operations`

Remove internal HTTP communication. Use direct application services and ports inside the monolith. Riot and Telegram remain external adapters.

## Technical Direction

- Favor Spring MVC and JPA for consistency.
- Keep external clients behind ports.
- Introduce an explicit scheduler component for polling.
- Replace startup side effects with controlled application workflows.
- Keep DTOs, entities, and domain models separated by responsibility.

## Target Data Model

### Players

Store:
- Riot game name
- Riot tag line
- resolved `puuid`
- active flag
- timestamps
- last sync status fields useful for UI

### Tracked Matches

Store only reduced match data:
- player reference
- `matchId`
- champion
- result
- queue or mode
- duration
- match end timestamp
- notification status
- created timestamp

### Poll Runs

Store lightweight operational audit data:
- started at
- finished at
- status
- counters
- summarized error if any

## Operational Flow

1. Scheduler starts a poll run using the global interval.
2. Active players are loaded from the database.
3. Riot account data and recent match identifiers are fetched.
4. Existing matches are filtered by `matchId`.
5. New matches are mapped to reduced persisted records.
6. Telegram messages are built and sent for new matches.
7. Successful notifications mark the match as notified.
8. Poll run results are recorded for UI and diagnostics.

## UI Scope

Provide a simple server-rendered UI for:
- list players,
- create player,
- edit player,
- activate or deactivate player,
- inspect recent runs,
- inspect recent tracked matches,
- trigger an on-demand poll if useful during operations.

## Reliability

Basic reliability only:
- simple retries for Riot and Telegram calls,
- clear structured-enough logs,
- deduplication by persisted match identity,
- no queue or outbox in this phase.

## Observability

- Actuator enabled.
- Clear logs around poll runs, player syncs, matches detected, and notifications sent or failed.

## Testing Strategy

Required validation:
- unit tests for domain and service logic,
- integration tests for persistence and web layers,
- end-to-end tests of the main polling-to-notification flow without live Riot,
- UI tests for management flows.

## Subagent System

### Analysis

- `architecture-analyst`: structure, boundaries, coupling, migration direction.
- `performance-analyst`: expensive I/O, scheduling cost, query strategy, payload handling.
- `cohesion-analyst`: duplicated responsibilities, misplaced abstractions, simplification opportunities.

### Implementation

- `domain-refactor-agent`: use cases, ports, application services.
- `persistence-refactor-agent`: entities, repositories, schema model.
- `integration-refactor-agent`: Riot and Telegram adapters, config cleanup.
- `ui-ops-agent`: server-rendered UI and management endpoints.
- `quality-agent`: automated tests and validation harness.
- `review-agent`: review, defect detection, and iteration guidance.

## Decision Summary

- Deep restructuring is allowed.
- Player source must be manageable, not static file based.
- UI must be server rendered.
- Polling remains globally configured and simple.
- Telegram remains global.
- Match persistence remains reduced.
- Validation must be strong before completion.

