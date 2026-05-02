# Round 02 Decisions

## Inputs

- User request on 2026-05-02 to improve the UI, configure tokens/API keys from the frontend, store them securely, and choose Riot region from the frontend.
- Approved design in `docs/plans/2026-05-02-secure-runtime-config-ui-design.md`.

## Decisions

- Store Riot and Telegram secrets in PostgreSQL encrypted with AES/GCM.
- Keep the encryption master key outside the database through `APP_CONFIG_ENCRYPTION_KEY`.
- Treat blank secret fields in the UI as "keep existing value".
- Show only configuration status, never decrypted secret values.
- Replace configurable Riot base URL with a selected Riot regional route.
- Support `EUROPE`, `AMERICAS`, `ASIA`, and `SEA` regional routes.
- Preserve environment variables as fallback values for existing deployments.
- Keep the dashboard server rendered.

## Rejected Alternatives

- Storing secrets as plain text in the database: rejected because it does not meet the secure-storage goal.
- Keeping only `.env` secrets: rejected because the user explicitly wants frontend editing.
- Letting users type arbitrary Riot base URLs: rejected because regions are a bounded Riot concept and free-form URLs are easier to misconfigure.
- OS keystore integration: rejected for now because Docker/NAS-style deployment should stay simple.

## Consequences

- Operators must keep `APP_CONFIG_ENCRYPTION_KEY` stable across restarts and backups.
- Existing `RIOT_API_BASE_URL` deployment configuration becomes obsolete.
- Tests must cover both env fallback and persisted encrypted settings.
