# Telegram Message V2 And Error Handling Design

Date: 2026-05-04
Status: Approved

## Objective

Extend Telegram notifications with rank-derived elo statistics and prevent user-facing Whitelabel error pages.

## Scope

- Fetch current ranked data from Riot using platform-scoped Summoner-V4 and League-V4 endpoints.
- Store a reduced rank snapshot on each player.
- Add player rank and roster average rank to Telegram notification stats when available.
- Keep rank lookup best-effort so notification delivery does not fail because Riot rank is unavailable.
- Add MVC global error handling that logs exceptions and returns the dashboard with a generic toast-style error.

## Message V2 Direction

The Telegram result card keeps the v1 local match statistics and adds:

- current ranked tier/division/LP for the player,
- numeric elo score behind the scenes,
- average roster elo based on stored player snapshots,
- delta versus roster average.

The score is internal only. The message shows rank labels, not raw implementation details.

## Data Flow

During notification stats creation, the application attempts to refresh the current player's rank. The rank snapshot is persisted on `PlayerEntity`.

Roster average is calculated from active players with a stored rank score.

If rank data is missing, the message says rank data is not available and still sends the match result.

## Error Handling

A global MVC exception handler logs unexpected exceptions with stack traces. For HTML dashboard requests it returns to the dashboard with `errorMessage = "Un error ha ocurrido"`, avoiding the Spring Whitelabel page.

Existing validation errors and expected domain errors continue to use their current field or flash messages.

## Testing

- Unit-test rank scoring and average calculations.
- Unit-test v2 Telegram message rendering with and without rank data.
- MVC-test unexpected dashboard action failures.
- Run the full Maven suite.

## Assumptions

- Riot League ranked entries contain tier, rank, league points, queue type, wins, and losses.
- SoloQ is preferred over Flex when both entries exist.
- A missing ranked entry means the player is unranked, not an application failure.
