# Cohesion Analysis

## Scope

Check whether the UI redesign affects module cohesion.

## Findings

- Dashboard composition belongs to the `ops` web layer.
- Player, match, settings, and polling services already provide view data.
- No domain behavior should be added for purely presentational changes.

## Recommendations

- Keep display-only grouping in the template.
- Add controller/view model fields only if needed for meaningful UX, not for styling.

## Risks

- Pulling UI-specific formatting into services would weaken cohesion.

## Handoff Items

- Prefer template-level presentation changes.
- Preserve existing service contracts.
