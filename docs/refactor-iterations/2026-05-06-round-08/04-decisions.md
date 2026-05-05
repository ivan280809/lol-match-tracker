# Decisions

## Decision 1: Archive Players With `archived_at`

Players will not be physically deleted. A new nullable `archived_at` column marks logical archive state.

Reason: it preserves player identity, match history, poll/audit context, and notification history while keeping archived players out of polling.

Rejected alternative: reuse `active=false` as archive. It cannot distinguish temporary inactive players from intentionally archived players.

## Decision 2: Keep UI Server-Rendered With Query Params

Dashboard filters and sorting will use GET query parameters and Thymeleaf rendering.

Reason: shareable URLs and no new frontend tooling match the current monolith.

Rejected alternative: add JavaScript client state. It adds complexity without improving this operator workflow.

## Decision 3: Add Persisted External Check Logs

Riot validation, account lookup, and Telegram test actions will record compact safe outcomes in a new external call log table.

Reason: the health panel and audit page need "last OK" and recent external response summaries without reading logs or re-calling integrations on page load.

Rejected alternative: only show flash messages. Flash messages disappear and do not support operations/audit.

## Decision 4: Account Validation Is Conditional On Riot Configuration

Create/update flows will resolve PUUID before saving when a Riot API key is configured. If Riot is not configured, they can save the player with no PUUID or preserve the existing PUUID and show clear UI state.

Reason: runtime configuration is editable in the UI; lack of a key should not block roster maintenance.

Rejected alternative: always require Riot resolution. It makes the dashboard unusable during initial setup or key outages.

## Decision 5: Use Current Reduced Match Model For Stats

Recent form, winrate, champion usage, and detail history will use the existing reduced `tracked_matches` fields.

Reason: the request asks for statistics available with the current model and avoids the larger global `Match` + `PlayerMatch` migration.

Rejected alternative: expand the match schema now. That belongs in a separate data-model iteration.

## Decision 6: Keep New Persistence Compatible With `ddl-auto`

New fields/tables will use JPA mappings compatible with the current `ddl-auto=update` setup.

Reason: the repository has not adopted Flyway/Liquibase yet.

Rejected alternative: introduce Flyway during this UI/ops pass. It is valuable but would widen the iteration beyond the requested dashboard work.
