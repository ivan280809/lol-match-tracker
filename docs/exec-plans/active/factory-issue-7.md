# Issue #7: P4: Agrupar consultas de deduplicación y escrituras de progreso

## Reparacion del 27 de octubre de 2026

Se reproducen dos fallos: el timer empieza a escribir al llegar a 5 s, por lo que
la confirmacion ya excede el plazo; ademas usa el scheduler primario de Telegram.
Se reserva un scheduler propio para progreso y 500 ms del plazo para ejecutar y
confirmar el write. El umbral de las llamadas directas sigue en 5 s: 100 llamadas
con reloj controlado en t=0..99 producen 20 flushes y un write final. Se prueba
tambien con el scheduler de Telegram ocupado. Se conserva deduplicacion, polling,
fallback al read model y las mediciones JPA reales. La copia de origen se conserva.

Plan the authorized Issue #7 recovery repairs while preserving batched deduplication and the existing read-model fallback. No owner decision is needed.

## Steps

- In PollRunService, serialize progress flushes per run or assign each in-flight flush a unique token so an earlier commit callback cannot clear markers for a newer flush. Add delayed-persistence coverage showing the newest player update is eventually stored.
- Coordinate flush callbacks and scheduled work with completeRun, failRun, and rateLimited. Prevent stale callbacks from restoring RUNNING progress after a terminal transition, and discard pending cached progress safely. Add a race/follow-up test that checks terminal status, final stage, cleared currentPlayer, and cache cleanup.
- Correct PollRunServiceProgressComparisonTest statement measurement scope. The supplied test captures batched statements after terminal completion but before four verification reads; either move capture after those reads and align totals, or report the earlier scope accurately. Keep entity update expectations at 101 baseline and 21 batched, and update assertion messages.
- Run focused progress tests and available PostgreSQL concurrency coverage. Update docs/exec-plans/reports/factory-issue-7-verification.md with the reproducible 100-player measurement, its statement-count scope, and any PostgreSQL coverage that could not run. Do not claim unexecuted coverage.
- Inspect the final diff to confirm PollingService batching, pagination and stop behavior, and findExisting read-model fallback remain unchanged.

## Acceptance

- Overlapping flushes cannot clear or delay newer pending progress; delayed persistence proves the newest player is eventually stored.
- Flushes racing with or following completeRun, failRun, or rateLimited cannot restore RUNNING state; terminal state and pending-cache cleanup are verified.
- The comparison test reports statement totals for a clearly stated measurement scope and retains update counts of 101 and 21.
- The 100-player comparison and progress visibility evidence are recorded accurately, including PostgreSQL test limitations.
- The existing polling batch-deduplication flow and findExisting fallback semantics remain intact.
