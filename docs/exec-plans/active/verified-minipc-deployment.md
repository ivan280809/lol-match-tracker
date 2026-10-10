# Verified pull deployment

User request: deploy only merged master code, preserve existing services, publish through the existing Cloudflare setup, replace self-hosted Actions execution with a local verified updater. Never merge pending PRs. User subsequently requested an email with final state and any required intervention.

Observed 2026-10-11: available host is Windows 10 / Docker Desktop amd64, whereas the repository describes a Linux miniPC with SWAG. No access to that destination or Cloudflare credentials was found. Do not assume this desktop is the production server.

Deliver a draft change with a GitHub-hosted test/build workflow, immutable release manifest, isolated Compose, secret setup, serialized Linux systemd updater, verified backups, conservative migration gate and rollback. Validate merged application image locally without real integrations. Do not activate production or merge this branch. Record remaining access/approval and end-to-end checks accurately.
