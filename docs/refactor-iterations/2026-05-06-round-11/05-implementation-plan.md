# Implementation Plan

## Step 1: P0 Rank Endpoint

- Update `RiotClient.fetchRankEntriesInternal` to call League-V4 by PUUID.
- Remove the unnecessary Summoner-V4 rank prefetch path.
- Update Riot adapter tests to assert the current endpoint and direct one-call behavior.

## Step 2: P1 Rank Product Semantics

- Keep `PlayerRankService.refreshRank` best-effort.
- Add a way for notification stats to express rank source/state without raw exception text.
- Update Telegram message tests for unranked/no-data/error copy.

## Step 3: P2 API-Backed Improvements

- Prefer low-risk documentation/configuration improvements over new live API calls.
- Document candidate ports, cache strategy, and rollout order for Status, Data Dragon, Spectator, Mastery, Timeline, and Challenges.

## Step 4: P3 Deferred Features

- Document low-priority API ideas and constraints: Clash, tournaments, RSO, replays, free champion rotation.

## Step 5: Validation

- Run focused tests around Riot rank, player rank, notification stats, and notification message formatting.
- Run full test suite.
- Run package build.
- Update `06-validation-report.md` with real commands and outcomes.
