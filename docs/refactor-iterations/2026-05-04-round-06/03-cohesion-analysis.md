# Cohesion Analysis

## Scope

Rank data ownership, notification formatting, and MVC exception handling.

## Findings

- Rank lookup belongs near Riot integration and player persistence, not in the message factory.
- Message factory should receive display-ready stats.
- MVC error handling is an ops/web concern.

## Recommendations

- Add rank records under the player or integration boundary.
- Keep score conversion deterministic and unit-tested.
- Keep the global exception handler small and generic.

## Risks

- Putting live Riot calls directly in message formatting would blur boundaries.

## Handoff

Add a rank snapshot service, extend notification stats, and add an MVC advice class.
