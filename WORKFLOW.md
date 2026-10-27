# Factory workflow for LoL Match Tracker

Issues are the unit of work. The trusted supervisor at `D:\software-factory` owns admission, configuration, processes, validation and publication. Editing this explanation inside a worktree does not grant permissions.

An owner-authored issue becomes executable when the owner applies `factory:ready`. The supervisor prepares a dedicated worktree and execution plan. Luna plans; local Ollama implements and self-reviews; trusted tests run; Luna independently reviews. Roles are sequential, at most medium effort.

States: BACKLOG, READY, RUNNING, WAITING, HUMAN_REVIEW, DONE, FAILED, CANCELLED. Errors feed bounded repairs. Missing criteria, resources, quota or access cause durable waits. Reply `factory:resume` and your clarification; an in-flight scope edit requires confirmation. `factory:cancel` cancels. `factory:retry` re-admits a failed task after correcting its cause.

Required validation is `./mvnw.cmd -B -ntp verify` with Java 26 and Docker: no errors, failures or skipped tests. Test reports and the verified SHA are evidence; model narrative is not proof. Preserve the modular monolith constraints.

Delivery is a draft PR. HUMAN_REVIEW is a successful handoff; the human decides integration. No automatic merge, deployment or API purchase. Closing the UI should not stop the supervisor; actual Windows startup limits are recorded separately.
