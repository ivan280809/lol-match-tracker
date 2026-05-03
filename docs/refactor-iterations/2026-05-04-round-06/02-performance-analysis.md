# Performance Analysis

## Scope

Live rank lookups and roster average calculation.

## Findings

- Rank refresh adds up to two Riot calls for the notified player.
- Roster average can use stored player snapshots and does not require live calls for every player.

## Recommendations

- Refresh only the current player's rank during notification.
- Calculate average from active players with stored rank scores.
- Treat rank lookup failures as best-effort and do not retry in this iteration.

## Risks

- First notification for a player may pay the rank lookup cost.
- Development Riot keys may expire or hit rate limits.

## Handoff

Implementation should log rank refresh failures and continue sending notifications.
