# Implementation Plan

1. Refactor `NotificationMessageFactory` to render a Telegram-compatible card layout.
2. Keep all existing data fields, but reduce visual noise with a summary block and a details block.
3. Add helper methods for aligned metrics and compact summary lines.
4. Update tests to assert the new card structure, escaping, shared match section, derived stats, and rank queue display.
5. Run focused tests, full tests if time allows, package, then update this iteration validation report.
