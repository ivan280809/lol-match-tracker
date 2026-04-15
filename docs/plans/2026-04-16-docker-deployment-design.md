# Docker Deployment Design

Date: 2026-04-16
Status: Approved

## Objective

Package the modular monolith so it can run on another device with Docker Compose, PostgreSQL, and external secrets supplied outside the repository.

## Requirements

- Keep the application buildable from the repository with `docker compose up --build`.
- Keep Riot and Telegram credentials out of versioned files.
- Allow both root `.env` usage and custom `--env-file` usage.
- Keep PostgreSQL included in the local deployment topology.
- Make startup more reliable with database health checks and restart policy.

## Options Considered

### Option A: Source-based Compose build

- Build the application image from the repository on the target device.
- Requires a checkout or copy of the repository on that device.
- Lowest operational complexity for now.

### Option B: Prebuilt registry image

- Publish the built image to Docker Hub or GHCR.
- The target device only needs Compose plus a secrets file.
- Better portability, but requires image publishing workflow that the project does not yet have.

## Selected Approach

Use Option A as the default repository workflow and document Option B as the next step if the user wants to avoid copying the repository to another device.

## Implementation Notes

- Add `.dockerignore` so `.env` and other local files do not enter the Docker build context.
- Provide `.env.example` as the safe template for required configuration.
- Refactor `docker-compose.yml` to use variable substitution rather than inline hardcoded secrets.
- Add PostgreSQL health check and service restart policy.
- Add a simple root `README.md` with deployment instructions.

## Operational Note

With the selected approach, the other device still needs the repository because Compose builds the image locally. To remove that requirement, publish a registry image in a later step.
