# Round 02 Implementation Plan

## Scope

Implement secure runtime configuration and a refreshed dashboard.

## Steps

1. Add settings persistence, encryption, forms, views, and region enum.
2. Update Riot and Telegram adapters to read active runtime settings.
3. Update dashboard controller with configuration rendering and save flow.
4. Redesign the Thymeleaf dashboard for operational scanning and configuration.
5. Replace `RIOT_API_BASE_URL` docs/env/compose references with `RIOT_API_REGION` and `APP_CONFIG_ENCRYPTION_KEY`.
6. Add and update automated tests.
7. Run the local Maven test suite.

## Validation Target

- Unit tests for crypto and settings service.
- MVC tests for dashboard and configuration save behavior.
- Existing service, integration, and polling tests remain passing.

## Assumptions

- PostgreSQL remains the operational data source.
- Security beyond local/LAN deployment remains out of scope, except secret encryption at rest.
- Existing env vars remain fallback values to avoid breaking first startup.
