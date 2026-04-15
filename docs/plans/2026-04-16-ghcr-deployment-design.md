# GHCR Deployment Design

Date: 2026-04-16
Status: Approved

## Objective

Allow the application to run on another device without copying the repository by publishing a ready-to-run Docker image to GitHub Container Registry.

## Requirements

- Publish a tested image automatically from `master`.
- Keep local Compose-based development unchanged.
- Provide a deployment Compose file that uses the published image instead of local build context.
- Keep secrets external through `.env` or `--env-file`.
- Document the difference between private and public GHCR package access.

## Selected Approach

- Add a GitHub Actions workflow that runs tests and pushes the Docker image to `ghcr.io/ivan280809/lol-match-tracker`.
- Keep `docker-compose.yml` for source-based local builds.
- Add `docker-compose.deploy.yml` for remote deployments using the published image.
- Reuse `.env.example` as the variable template, including `APP_IMAGE`.

## Deployment Flow

1. Push to `master`.
2. GitHub Actions runs tests.
3. If tests pass, the workflow builds and pushes the image to GHCR.
4. On another device, use `docker-compose.deploy.yml` plus an env file to start the stack.

## Operational Note

If the GHCR package remains private, the target device needs `docker login ghcr.io`. If the package is public, only Docker Compose and the env file are required.
