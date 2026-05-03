# Telegram Message Redesign

Date: 2026-05-04
Status: Approved

## Objective

Replace the plain Telegram result notification with a more visual, useful message that includes local match-history statistics.

## Scope

- Keep Telegram as the only notification channel.
- Format the message with Telegram HTML.
- Calculate statistics from persisted reduced match data.
- Avoid new Riot API calls in this iteration.
- Leave rank and elo averages for a later iteration because rank is not stored today.

## Message Direction

Use a compact result card:

- result header with player name,
- champion, mode, duration, and finished-at time,
- recent form from the latest saved matches,
- current streak,
- same-day wins and losses,
- champion-specific record and win rate,
- recent average duration.

The message should remain readable on mobile Telegram and avoid noisy long tables.

## Data Flow

`NotificationService` will request a stats snapshot for the tracked match, then pass both match and stats to `NotificationMessageFactory`.

The stats snapshot is read from `TrackedMatchRepository` using the same persisted reduced match data used by the UI and deduplication.

`TelegramNotifier` will send the message with `parse_mode=HTML`.

## Error Handling

Stats are best-effort. If there is little history, the message still sends with available values and clear zero-safe labels.

Telegram text will be HTML-escaped before sending so player names, champion names, and modes cannot break formatting.

## Testing

- Unit-test message formatting and HTML escaping.
- Unit-test stats calculations for recent form, streaks, daily record, champion record, and average duration.
- Update notification service tests for the new factory input.
- Run the Maven test suite.

## Assumptions

- Persisted match history is the source of truth for notification stats.
- Telegram supports HTML formatting in the configured bot flow.
- Elo/rank averages require Riot League data and a persistence model change, so they are intentionally out of scope here.
