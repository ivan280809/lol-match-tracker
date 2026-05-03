# Performance Analysis

## Scope

Repository reads needed to calculate Telegram notification statistics.

## Findings

- Notifications are sent only for newly discovered matches, not on every dashboard render.
- Current history volume is expected to be small.
- The existing recent-matches query already limits global UI history, but player-specific notification stats need bounded reads.

## Recommendations

- Use bounded repository queries for recent player history.
- Keep calculations in memory over a small result set.
- Avoid external Riot calls in the notification path for this iteration.

## Risks

- If a player accumulates large history and unbounded champion queries are added later, notification latency could grow.

## Handoff

Implementation should limit recent-history reads and keep champion/day calculations simple.
