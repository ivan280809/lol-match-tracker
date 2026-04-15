# Round 01 Architecture Analysis

## Scope

Analyze current module boundaries, lifecycle behavior, internal communication, and target migration path.

## Findings

- The project is deployed as one application but still behaves like collapsed microservices, with internal HTTP calls between player tracking, match history, and notification flows.
- Polling is started through `@PostConstruct` side effects and a reactive interval embedded in a service, which hides lifecycle ownership and makes execution harder to control and test.
- The operational player source is a static `players.json` resource, which blocks runtime management and forces code or config changes for normal operations.
- Match history processing and notification behavior are coupled to persistence updates, but the ownership is spread across controller, use case, and service layers.
- The codebase mixes reactive WebClient flows with blocking JPA persistence, increasing complexity without a clear architectural benefit for this use case.
- The current data model persists a very large Riot match payload tree, even though the target behavior only needs reduced data for deduplication, UI, and notification.
- Package structure exposes duplicated transport models across different internal modules instead of clear shared contracts or focused internal models.

## Recommendations

- Rebuild the application as a modular monolith with direct service collaboration instead of internal HTTP.
- Introduce explicit application modules for player management, match tracking, notification, and operations.
- Move polling ownership into a dedicated scheduler component with configuration-driven cadence.
- Replace static player loading with database-backed managed players and operational CRUD.
- Reduce the persistence model to the smallest useful match summary plus execution audit data.
- Standardize the technical style around Spring MVC plus JPA and keep external integrations behind ports.

## Risks

- The refactor will touch many packages and may invalidate much of the current test suite.
- Removing the large existing persistence model may require careful handling of current entity relationships.
- Lifecycle changes around polling can introduce regressions if not covered by integration and e2e tests.

## Handoff

- `domain-refactor-agent`: define module boundaries and scheduler ownership.
- `persistence-refactor-agent`: replace current payload-centric storage model.
- `integration-refactor-agent`: remove internal HTTP adapters and isolate external clients.
