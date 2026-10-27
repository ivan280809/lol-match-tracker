# Issue #5: P2: Reutilizar rango y estadísticas por lote de avisos

## Reparacion del 27 de octubre de 2026

El test de 20 avisos contaba tambien la lectura del roster del dispatcher al
arrancar el contexto. Se aisla ese scheduler en la prueba de medicion y se limpia
su historial antes de medir. Se retiran los condicionales de produccion que
deshabilitaban el dispatcher para un perfil de test. El flujo con scheduler real
se sigue comprobando en ApplicationFlowIntegrationTest. Se repetiran las pruebas
de rangos, colas mixtas, TTL, deadlines, errores y polling. No se cerrara la issue
sin la comprobacion completa con PostgreSQL.

Resolve the four listed conflict files by preserving issue #5 batch caching and the accepted notification deadline, rate-limit, and regression-test behavior. No unresolved product decision is apparent.

## Steps

- In src/main/java/com/loltracker/app/notification/NotificationService.java, remove all merge markers and combine the paths: keep rank and roster caches scoped to one dispatch batch, limit items by maximumMessages, enforce the deadline/minimum-send-window checks, and stop after rate limiting. Retain the deadline-aware dispatch path, using the cached rank and roster values when building stats; remove the obsolete legacy dispatch stub. Preserve outbox attempt, sent/failure recording, and metrics behavior.
- In src/test/java/com/loltracker/app/notification/NotificationServiceTest.java, resolve markers while retaining both the issue’s batch-cache coverage and upstream notification deadline/rate-limit coverage.
- In src/main/java/com/loltracker/app/tracking/PollingService.java, resolve conflicts in favor of accepted polling behavior and preserve any issue-required notification batching integration; inspect surrounding methods before removing either side.
- In src/test/java/com/loltracker/app/tracking/PollingServiceTest.java, preserve both accepted polling regressions and tests relevant to notification dispatch integration; remove only duplicate or superseded cases after checking assertions.
- Review all four files for conflict markers and ensure the merged call signatures and constructor wiring agree with NotificationStatsService and PlayerRankService APIs. Keep champion, queue, and position statistical windows unchanged.

## Acceptance

- All four named files contain no merge conflict markers and compile against the existing service APIs.
- A batch of 20 notifications for the same player reuses one rank lookup and one roster average lookup while respecting maximumMessages and deadline constraints.
- Telegram rate limiting stops further dispatch in the batch; expired send windows do not send messages; sent and failed outcomes continue to be recorded.
- Rank refresh/cache behavior, roster average reuse, Riot error fallback, and rank query timestamp display remain covered by existing tests.
- Polling and notification regression tests from both accepted sides remain intact, including behavior unrelated to issue #5.
- Statistical windows retain their established champion, queue, and position semantics.
