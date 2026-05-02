# Round 02 Architecture Analysis

## Scope

Assess how runtime configuration, secret storage, region selection, and dashboard improvements fit the modular monolith.

## Findings

- Riot and Telegram credentials currently come from environment-backed Spring properties only.
- Riot regional routing is controlled through a single `riot.api.base-url` value, which hides the actual product choice from users.
- The dashboard is already the operational entry point, so adding configuration there keeps the app cohesive.
- Sensitive data must not be rendered back to Thymeleaf after storage.
- External clients are already behind adapter classes, making the runtime-settings integration narrow.

## Recommendations

- Add a focused settings module with persistence, encryption, forms, and views.
- Keep the encryption master key outside the database.
- Make region selection an enum, not free text, to avoid invalid Riot URLs.
- Preserve environment variables as fallback for existing deployments.
- Keep UI server-rendered and simple.

## Risks

- Losing `APP_CONFIG_ENCRYPTION_KEY` prevents decrypting stored secrets.
- Existing deployments using `RIOT_API_BASE_URL` need migration documentation.
- Runtime settings failures can surface during polling if configuration is incomplete.

## Handoff

- Persistence work: create a single persisted configuration row and repository.
- Integration work: update Riot and Telegram adapters to read active settings.
- UI work: add configuration form and improve dashboard layout.
- Quality work: add tests for encryption, fallback, region routing, and dashboard flow.
