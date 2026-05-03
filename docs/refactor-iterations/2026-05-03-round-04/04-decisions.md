# Decisions

## Decision 1: Use A Visual League-Inspired Dashboard

The dashboard will move from a plain admin panel to a darker, more visual operations dashboard with restrained gold and blue accents.

Reason: the user explicitly prefers a more visual UI, and the project benefits from stronger hierarchy around players and recent match outcomes.

Rejected alternative: a dense operations console. It would be efficient, but it would not address the stated visual quality problem.

## Decision 2: Keep Thymeleaf And Existing Routes

The redesign will keep the current server-rendered Thymeleaf approach and existing form endpoints.

Reason: this matches the project architecture and avoids unnecessary frontend complexity.

Rejected alternative: adding React or another frontend stack. It would increase maintenance and deployment complexity without a clear benefit for this small dashboard.

## Decision 3: Keep Configuration Secondary

Configuration remains available on the dashboard but moves visually behind roster, recent matches, and system status.

Reason: configuration is important but less frequent than inspecting players, polling, and match results.
