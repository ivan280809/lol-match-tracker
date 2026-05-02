# Secure Runtime Configuration UI Design

Date: 2026-05-02
Status: Approved

## Objective

Allow operators to configure Riot and Telegram from the server-rendered UI while keeping sensitive values encrypted at rest and removing the fixed Riot regional base URL from application properties.

## Scope

- Add UI controls for Riot API key, Riot region, Telegram bot token, and Telegram chat id.
- Store secret values encrypted in PostgreSQL.
- Keep the encryption master key outside the database through `APP_CONFIG_ENCRYPTION_KEY`.
- Use environment variables as startup fallback values when no database value exists.
- Replace `RIOT_API_BASE_URL` with a selectable Riot regional route.
- Improve the dashboard layout while keeping it simple and server rendered.

## Architecture

Introduce a small `settings` module with:

- a JPA entity for persisted application configuration,
- a repository and service for reading/updating settings,
- a crypto service using AES/GCM with a random IV per value,
- a Riot region enum that owns the allowed regional routing URLs.

Riot and Telegram adapters read runtime settings through the settings service. Controllers never receive decrypted values back for rendering; the UI only shows configured/not configured status.

## Security

Secrets are encrypted before persistence. The database stores encrypted payloads only. The master key comes from `APP_CONFIG_ENCRYPTION_KEY`; changing or losing it makes existing encrypted values unreadable. The UI treats blank secret fields as "keep existing value" so secrets are not echoed back into HTML.

## Data Flow

1. Operator opens the dashboard.
2. Dashboard shows configuration status and a region select.
3. Operator submits a new key/token/chat id or leaves sensitive fields blank.
4. The settings service encrypts provided secrets and saves the selected region.
5. Riot and Telegram clients read the active runtime settings during calls.

## Validation

- Unit tests cover encryption, settings fallback/update behavior, and region base URL resolution.
- MVC tests cover dashboard configuration rendering and update flow.
- Existing polling and application flow tests must remain green.
