# Round 02 Cohesion Analysis

## Scope

Check responsibility placement for configuration UI, secret encryption, regional routing, and integration adapters.

## Findings

- Secret handling does not belong in Riot or Telegram adapters.
- Region URL construction should be centralized to avoid free-form URL handling.
- Dashboard rendering should receive only safe configuration status.
- The settings module can serve both UI and integrations without internal HTTP calls.

## Recommendations

- Put encryption and persisted configuration in `com.loltracker.app.settings`.
- Expose safe view/form objects for controllers.
- Let `RiotRegion` own allowed regional base URLs.
- Keep adapters focused on external HTTP calls.

## Risks

- Mixing decrypted runtime settings into view models would leak sensitive values.
- Allowing arbitrary base URLs from the frontend would reintroduce fragile configuration.

## Handoff

- UI forms must never prepopulate secret inputs.
- Integration adapters must request runtime settings from the settings service.
