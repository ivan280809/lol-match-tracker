# V2 Release Hardening Design

## Purpose

Round 10 turns the Round 09 readiness review into an implementation pass for a distributable V2. The goal is to remove the biggest release blockers while keeping the application a cohesive Spring MVC + JPA monolith.

## Scope

- Introduce versioned database migrations and move production away from `ddl-auto=update`.
- Preserve existing history by migrating legacy `tracked_matches` into `matches` and `player_matches`.
- Move operational reads toward the global match model where the current UI/statistics can do so safely.
- Add PostgreSQL-oriented migration/test coverage, browser-style end-to-end coverage where infrastructure is available, and stronger MVC coverage where it is not.
- Add minimal operational hardening: metrics, Docker healthcheck, CI artifacts, and optional dashboard access guard.
- Add shared-match product improvements: shared statistics, queue display mapping, multi-region operator copy, and rate-limit history visibility.

## Architecture

The app stays a modular monolith:

- `settings` owns runtime configuration and operational flags.
- `player` owns Riot identity and PUUID uniqueness semantics.
- `match` owns global match and participation persistence.
- `notification` owns outbox and Telegram message formatting.
- `ops` owns dashboard, audit, poll runs, locks, and operational health.
- `integration` owns external adapters behind ports.

No internal HTTP calls are introduced.

## Data Design

Flyway becomes the production schema owner. The first migration creates or completes the current schema, including Round 09 tables and indexes. A follow-up migration backfills `matches` and `player_matches` from `tracked_matches` using idempotent SQL.

Production defaults to `spring.jpa.hibernate.ddl-auto=validate`. Tests can still use generated schemas where appropriate, but migration tests must cover the Flyway scripts.

PUUID uniqueness is enforced with a filtered/partial unique index where the database supports it. Service-level duplicate checks remain as product-friendly validation.

## Operations

Polling remains globally configurable. Operators get:

- active run and rate-limit pause state,
- rate-limit history in audit/dashboard,
- queue labels instead of only queue ids,
- explicit note that the current product uses one Riot regional routing cluster plus per-player platform routing.

Minimal dashboard access control is opt-in via environment configuration so local/LAN installs do not break by default.

## Testing

Round 10 adds:

- migration validation,
- PostgreSQL Testcontainers coverage when Docker is available,
- browser-style E2E coverage when browser tooling can be installed/run locally,
- MVC fallback tests for the same operator flows,
- tests for read-model migration, shared stats, queue display, metrics hooks, and access guard.

If browser or Docker infrastructure is unavailable locally, the validation report must say so explicitly and keep the tests skip-safe rather than pretending full coverage ran.

## Risks

- Full migration from legacy match reads can touch many views and statistics; implementation should switch bounded read paths first and keep compatibility services available.
- PostgreSQL-specific partial indexes need a safe H2/test fallback.
- Adding security must remain optional to avoid locking out existing local deployments.
