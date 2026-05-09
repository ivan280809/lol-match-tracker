# Implementation Plan

1. Add Flyway V4 indexes for Telegram history queries.
2. Make deploy compose explicitly enable Flyway.
3. Add notification records for performance profile and performance delta.
4. Add repository methods for recent, queue, and position windows.
5. Extend `NotificationStatsService` to compute profiles, deltas, highlights, and shared combined context.
6. Extend `NotificationMessageFactory` with compact sections.
7. Update unit tests for stats and Telegram rendering.
8. Run focused tests, full tests, and package.
9. Push and validate GitHub Actions deploy.
