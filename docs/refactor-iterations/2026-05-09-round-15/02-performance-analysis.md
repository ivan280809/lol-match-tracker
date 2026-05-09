# Performance Analysis

## Scope

Assess the cost of improving Telegram card rendering.

## Findings

- Formatting is in-memory string assembly and runs once per notification.
- No additional Riot, Telegram, or database calls are needed.
- The message must stay under Telegram `sendMessage` text limits after entity parsing.

## Recommendation

- Prefer compact lines and aligned monospace tables.
- Avoid duplicating all existing sections in both summary and detail.
- Keep historical profiles as short one-line summaries.

## Risks

- Richer text can become too long for extreme shared matches or many highlights.

## Handoff

Keep the final output compact and bounded. Tests should assert the presence of the new structural tags and escaping.
