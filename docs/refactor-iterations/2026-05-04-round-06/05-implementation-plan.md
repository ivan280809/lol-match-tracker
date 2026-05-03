# Implementation Plan

## Scope

Telegram v2 rank statistics and generic MVC error handling.

## Steps

1. Extend player persistence with rank snapshot fields.
2. Add platform Riot base URLs and rank response records.
3. Add rank snapshot service and score conversion.
4. Extend notification stats and message rendering.
5. Add MVC global exception handler and toast styling.
6. Update tests.
7. Run Maven tests and local Docker validation.
8. Commit and push the branch.

## Tests

- Notification message factory tests.
- Rank snapshot service tests.
- Dashboard MVC error test.
- Full Maven suite.

## Risks

- Live Riot rank calls depend on the current API key.
- Schema auto-update must add nullable player rank columns in PostgreSQL.
