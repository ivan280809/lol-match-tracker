# Cohesion Analysis

## Scope

Notification formatting and local match-history statistics.

## Findings

- `NotificationMessageFactory` should stay responsible for text composition.
- Stats calculation is separate behavior and should not be embedded in the factory.
- `TelegramNotifier` should only translate message content into Telegram API payloads.

## Recommendations

- Introduce immutable stats records in the notification package.
- Keep repository access out of the message factory.
- Keep delivery configuration in the Telegram adapter.

## Risks

- Adding too many statistics directly to the factory would make tests brittle and reduce readability.

## Handoff

Implementation should keep clear boundaries: stats service, message factory, notifier adapter.
