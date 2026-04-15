# Round 01 Decisions

## Inputs

- Design document in `docs/plans/2026-04-04-lol-tracker-refactor-design.md`
- Analysis artifacts from this round

## Decisions

- Keep the system as a single Spring Boot application.
- Remove internal HTTP calls between internal modules.
- Replace the operational use of `players.json` with managed player persistence.
- Build a simple server-rendered management UI in the same application.
- Persist a reduced match model instead of the full Riot payload.
- Use a global scheduler configuration for polling.
- Keep Telegram configuration global.
- Prefer a cohesive MVC + JPA model for the refactor target.
- Require strong automated validation before closing the refactor.

## Rejected Alternatives

- Keep the current pseudo-microservice communication inside the monolith.
- Preserve the full Riot payload in the database.
- Add authentication in the first refactor wave.
- Introduce queue or outbox complexity in the first wave.
- Build a separate SPA frontend.

## Consequences

- A large package and model reorganization is expected.
- Existing DTO and entity trees will likely be removed or drastically simplified.
- Tests must be rebuilt around the new module boundaries.
- UI and operational flows become part of the core application scope.
