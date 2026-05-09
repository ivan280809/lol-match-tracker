# Validation Report

## Status

Implemented and locally validated.

## Commands

- `.\mvnw.cmd "-Dtest=NotificationMessageFactoryTest" test`
- `.\mvnw.cmd test`
- `.\mvnw.cmd -DskipTests package`

## Results

- Focused Telegram formatter tests: PASS.
  - 4 tests executed.
  - Covered the new card layout, HTML escaping, shared-player rendering, derived context, comparison and highlights.
- Full suite: PASS.
  - 205 tests executed.
  - 0 failures.
  - 0 errors.
  - 2 skipped existing PostgreSQL/Testcontainers tests because Docker is not available locally.
- Package: PASS.
  - Built `target/lol-match-tracker-1.0.0.jar`.
- Remote CI/deploy: PASS.
  - Run `25611761081`.
  - CI tests: PASS.
  - Package: PASS.
  - GHCR image build and push: PASS.
  - Deploy mini PC: PASS.
  - Application health check: PASS.
  - URL: `https://github.com/ivan280809/lol-match-tracker/actions/runs/25611761081`.

## Implementation Notes

- Telegram Bot API supports basic message formatting with HTML entities, but not browser CSS in message text.
- The card was implemented with supported HTML:
  - `blockquote` for the summary,
  - `pre` for aligned performance metrics,
  - `blockquote expandable` for lower-priority match details,
  - existing `b`, `i`, and `code` tags for emphasis.
- No Flyway migration was required for this iteration because no persisted data model changed.

## Final Distributable Readiness Review

- P0: None introduced by this change.
- P1: Add a real Telegram preview/test-send fixture in the dashboard so operators can inspect the rendered message before enabling notifications.
- P2: Add an optional image-card notification path only if text cards are not enough; this would need rendering/upload infrastructure.
- P3: Consider configurable notification verbosity once more users/players are tracked.

## Residual Risks

- Telegram client rendering can vary slightly between platforms.
- Very narrow mobile screens can still wrap `pre` rows, although the table is intentionally compact and ASCII-only.
