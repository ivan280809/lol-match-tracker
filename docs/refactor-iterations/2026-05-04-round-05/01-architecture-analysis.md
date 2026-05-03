# Architecture Analysis

## Scope

Telegram notification content for newly tracked matches.

## Findings

- Notification creation is centralized in `NotificationMessageFactory`.
- Delivery is isolated in `TelegramNotifier`.
- Persisted match data already contains enough fields for local form, streak, champion, and duration statistics.
- Rank and elo are not currently persisted.

## Recommendations

- Add a small notification stats service in the notification module.
- Read match history through `TrackedMatchRepository`; do not introduce internal HTTP calls.
- Keep Telegram formatting concerns inside notification/integration code.
- Treat rank and elo as a later extension with explicit Riot League integration and persistence.

## Risks

- Message formatting can break if dynamic text is not escaped.
- Stats can be misleading if calculated from a very small history, so labels should be explicit.

## Handoff

Implementation should create a stats snapshot type, calculate it from stored matches, and update the factory/notifier tests.
