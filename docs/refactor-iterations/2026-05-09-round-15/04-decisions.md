# Decisions

## Decision 1: Telegram HTML Only

Use Telegram-supported HTML entities only. Do not add CSS, inline styles, or browser-only HTML because Telegram Bot API message formatting does not support them.

Rejected alternative: generate browser HTML with CSS and send it as text. It would not render as a card in Telegram.

## Decision 2: Text Card, Not Image Card

Keep the notification as a text message with HTML formatting. Image generation would require rendering infrastructure, uploads, file lifecycle handling, and more failure modes.

Rejected alternative: send a generated PNG card. It can look better visually, but it is a larger architecture change and less reliable for match alerts.

## Decision 3: Compact Card V3 Layout

Use a stronger Telegram-native structure:

- title line with result and player,
- blockquote summary,
- monospace metrics table,
- compact form/context/rank sections,
- expandable blockquote for match metadata.

## Decision 4: ASCII-First

Keep message decorations ASCII-first to match repository editing rules and avoid inconsistent emoji/custom emoji rendering.

## Validation

Update unit tests for the factory and run the focused notification tests plus package.
