# Implementation Plan

## Scope

Redesign the existing dashboard template with improved visual hierarchy and usability.

## Steps

1. Update `dashboard.html` CSS variables, layout, cards, forms, and responsive behavior.
2. Rework the top command area and summary metrics.
3. Convert the players table into roster cards.
4. Convert recent matches into richer match rows.
5. Keep poll runs and configuration in secondary panels.
6. Update MVC tests if visible copy changes.
7. Run the test suite.

## Tests

- Existing `DashboardControllerTest`.
- Full Maven test suite when feasible.

## Risks

- Long player names, errors, or timestamps may overflow compact cards.
- The single-template approach may become large, but it keeps this iteration contained.
