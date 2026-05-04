# Self-Hosted Runner Deployment Design

Date: 2026-05-04
Status: Approved

## Objective

Deploy the Dockerized monolith to a Linux mini PC automatically after a successful push to `master` or `main`, with an optional manual run from those branches, without exposing SSH or storing application secrets in GitHub.

## Requirements

- Run tests before publishing or deploying.
- Publish the application image to `GHCR`.
- Deploy only after the image is published successfully.
- Run the deploy job on a self-hosted GitHub Actions runner installed on the mini PC.
- Target a runner with labels `self-hosted` and `minipc`.
- Use `docker-compose.deploy.yml` and a stable Compose project name: `lol-match-tracker`.
- Keep the real env file on the mini PC at `/opt/lol-match-tracker/lol-tracker.env`.
- Keep Riot, Telegram, database, and encryption secrets out of the repository and out of GitHub Actions secrets.

## Selected Approach

Use a two-job GitHub Actions workflow:

1. `test-and-publish` runs on GitHub-hosted Ubuntu, executes Maven tests, builds the Docker image, and pushes both `latest` and a commit-SHA tag to `GHCR`.
2. `deploy-minipc` runs on the mini PC self-hosted runner, copies the Compose file into `/opt/lol-match-tracker`, pulls the commit-specific image, restarts the stack, and checks `/actuator/health`.

The deployment job exports `APP_IMAGE` with the commit-specific tag from the publish job. This keeps `latest` useful for manual operations while making automated deployments deterministic.

## Alternatives Rejected

- SSH from GitHub Actions into the mini PC: rejected because the mini PC is expected to be reachable only from the home network, and opening SSH publicly is unnecessary risk.
- Watchtower-only deployment: rejected because it decouples deployment from test and publish success, making failures harder to trace.
- Storing runtime secrets as GitHub Actions secrets: rejected because the application already supports an external env file, and the target machine can keep those secrets locally.

## Operational Notes

- The runner user must be able to run Docker commands without `sudo`.
- `/opt/lol-match-tracker` must exist and be writable by the runner user.
- `/opt/lol-match-tracker/lol-tracker.env` must exist before the first deploy.
- The application listens on container port `8080`, but the mini PC host publishes it only on `127.0.0.1:8085`.
- If the GHCR package is private, the workflow logs in with `GITHUB_TOKEN` before pulling.
- Manual pulls from the mini PC may still require either a public package or `docker login ghcr.io`.
