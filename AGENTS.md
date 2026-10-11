# LoL Match Tracker: project map

Java 26 / Spring Boot modular monolith with PostgreSQL, Riot polling and Telegram. Preserve the cohesive monolith and behavior unless the admitted issue changes it.

- Architecture and approved decisions: docs/plans/.
- Historical refactors and validation: docs/refactor-iterations/.
- External API reference: docs/reference/riot-lol-api.md.
- Factory protocol and user guide: WORKFLOW.md and docs/factory/README.md.
- Nontrivial new tasks keep plans in docs/exec-plans/active/.

Use application services, ports/adapters and persistence boundaries. No internal HTTP self-calls, second operational player store or microservices. Telegram and polling settings remain global. Keep server-rendered UI and explicit environment configuration.

One task and one sequential executor. project-agents/ contains historical descriptions, not live workers or mandatory delegation. Six-document bundles apply to historical refactor waves; routine factory tasks use approved plans and verification evidence.

Run ./mvnw.cmd -B -ntp verify with Java 26. PostgreSQL tests require Docker; skipped tests are incomplete release evidence. Avoid real Riot/Telegram calls. Record changed files, meaningful checks and residual risks. Never publish credentials.
