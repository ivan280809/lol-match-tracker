# Decisions

## Decision 1: Use Only Persisted Match Data

Round 14 will add recent, queue, champion, position, comparison, highlight, and shared KDA context using local persisted match rows only.

Reason: Telegram must stay reliable and avoid Riot rate-limit pressure.

Rejected alternative: call Match-V5 timeline, Data Dragon, Spectator, mastery, or other Riot endpoints during notification dispatch. That increases latency and failure risk.

## Decision 2: Add Compact Context Sections

The Telegram card will add compact "Contexto" and "Comparativa" sections instead of expanding every metric into long prose.

Reason: Telegram messages must stay readable on mobile.

Rejected alternative: send a full report with every possible average. It would bury the result.

## Decision 3: Use Flyway V4 For Query Support

Flyway V4 will add indexes for the bounded history queries used by Telegram.

Reason: this keeps deployment schema changes explicit and production-friendly.

Rejected alternative: rely only on Hibernate-generated schema. The project uses Flyway + validate for deploy.

## Decision 4: Force Flyway Enabled In Deploy Compose

The deploy compose file will explicitly set `SPRING_FLYWAY_ENABLED=true` and related Flyway settings.

Reason: the last deployment failed with `missing table [player_rank_snapshots]`, indicating the production container validated before migrations were available/applied.

Rejected alternative: change production to `ddl-auto=update`. That weakens schema discipline.
