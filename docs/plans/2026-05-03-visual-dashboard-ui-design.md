# Visual Dashboard UI Design

Date: 2026-05-03
Status: Approved

## Objective

Redesign the server-rendered dashboard so the application feels more like a League of Legends tracking app while remaining useful for local operations.

## Scope

- Keep the UI as a simple Thymeleaf dashboard.
- Improve visual hierarchy, spacing, status readability, and action clarity.
- Make players and recent matches more prominent than configuration.
- Keep Riot and Telegram configuration available without making it the main visual focus.
- Avoid new frontend build tooling or client-side framework dependencies.

## Direction

Use a visual dashboard inspired by League's competitive tone:

- dark operational surface,
- restrained gold and blue accents,
- prominent metric tiles,
- roster-style player cards,
- richer recent match rows,
- compact operations and configuration panels.

The page should open directly to the usable dashboard, not a marketing hero.

## Layout

The first viewport contains:

- a top command area with the app name, system status, and polling action,
- summary metrics for players, matches, Riot region, and Telegram readiness,
- a primary content area split between player roster and recent matches,
- a secondary operations area for configuration and poll run history.

## Data Flow

The current controller model is sufficient:

- `dashboard` for summary counts,
- `players` for roster cards,
- `matches` for recent match rows,
- `runs` for poll history,
- `configuration` and `configurationForm` for runtime settings.

No new endpoint or client-side state is required.

## Error Handling

Existing success and error flash messages remain server-rendered. Player-level errors should be easier to scan inside the roster. Empty states should remain explicit but visually calmer than errors.

## Testing

- Update MVC assertions only if visible copy changes.
- Run the local test suite.
- When possible, run the app and visually inspect the dashboard in a browser.

## Assumptions

- The dashboard is mostly used by a small operator audience.
- A more visual style is preferred over a dense admin-console feel.
- Security remains out of scope.
