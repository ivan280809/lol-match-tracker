# Architecture Analysis

## Scope

Improve the Telegram match notification card aesthetic without changing the notification delivery architecture.

## Current Shape

- `NotificationMessageFactory` owns all Telegram message composition.
- `TelegramNotifier` sends the generated text with `parse_mode=HTML`.
- The monolith already keeps Telegram behind `TelegramNotificationPort`.
- Stored match and derived stats data are already available in `NotificationStatsSnapshot`.

## Findings

- Telegram Bot API HTML is not browser HTML; CSS is not supported in message text.
- A polished card must be built with supported entities: `b`, `i`, `code`, `pre`, `blockquote`, links and safe escaping.
- The current message is correct but visually flat: many sections compete with equal weight.

## Recommendation

Keep the formatter server-side and introduce a Telegram-compatible "card" layout:

- strong title,
- compact summary blockquote,
- aligned metric table in `pre`,
- concise sections with consistent labels,
- expandable details block for low-priority match metadata.

## Risks

- Some old Telegram clients may render blockquotes less prominently, but text remains readable.
- `pre` tables need compact ASCII content to avoid awkward wrapping on narrow screens.

## Handoff

Implement only message rendering and tests. No persistence or integration changes are required.
