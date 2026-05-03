# Implementation Plan

## Scope

Improve Telegram match notifications with visual formatting and local statistics.

## Steps

1. Add a notification stats snapshot and stats service.
2. Add bounded player-history repository query methods.
3. Update `NotificationMessageFactory` to render the rich HTML message.
4. Update `NotificationService` to request stats before building the message.
5. Update `TelegramNotifier` to send `parse_mode=HTML`.
6. Add and update unit tests.
7. Run the Maven test suite.

## Tests

- `NotificationMessageFactoryTest`
- New stats service test
- `NotificationServiceTest`
- Full Maven test suite

## Risks

- Telegram HTML escaping must cover all dynamic values.
- Existing tests may assert exact plain text and need intentional updates.
