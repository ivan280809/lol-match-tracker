# Decisions

## Decision 1: Use Telegram HTML

Messages will use Telegram HTML formatting.

Reason: HTML is readable, easier to escape safely than MarkdownV2, and enough for bold labels, compact sections, and visual result cards.

Rejected alternative: keep plain text. It does not address the user's request for a prettier message.

## Decision 2: Calculate Only Local Statistics

This iteration will calculate stats from persisted reduced match data.

Reason: the data is already available, fast, and reliable inside the monolith.

Rejected alternative: calculate elo averages now. Elo/rank is not stored and would require Riot League calls plus a persistence model change.

## Decision 3: Keep Notification Responsibilities Split

Stats calculation, message composition, and Telegram delivery remain separate components.

Reason: this keeps the modular monolith clean and makes the behavior easier to test.
