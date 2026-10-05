# Issue #6: P3: Separar el despacho de Telegram del ciclo de polling

La inspección confirma que el alcance aprobado está delimitado y no requiere otra decisión funcional. El plan existente cubre dispatcher programado en el monolito, límites y reintentos, métricas y pruebas; la evidencia registrada conserva los mecanismos de polling y documenta las limitaciones de verificación PostgreSQL.

## Steps

- Continuar desde el worktree preservado; mantener el cambio enfocado y el diff total por debajo de 60 KB.
- Separar el envío del polling mediante la outbox durable y el dispatcher programado existente, conservando lock PostgreSQL, rate limit, paginación, caché, métricas, filtros y manejo de errores del polling.
- Conservar deduplicación, persistencia, siguiente intento y backoff de 429; limitar cada pasada por mensajes y duración sin introducir claims locales duplicados ni prometer exactly-once.
- Completar las pruebas simuladas de lentitud, caída, pausa y reanudación, incluida la demostración de que polling termina y la notificación sigue en outbox mientras Telegram está bloqueado.
- Registrar duración del polling y antigüedad de avisos pendientes; ejecutar Maven verify y pruebas PostgreSQL con Docker, y solicitar revisión independiente del diff completo revisable.

## Acceptance

- El despacho de la outbox es independiente del ciclo de polling y permanece dentro del monolito.
- Cada pasada está limitada por cantidad y tiempo; se respetan siguiente intento y backoff de 429.
- Persistencia, reintentos y deduplicación se mantienen con las protecciones PostgreSQL existentes.
- Pruebas simuladas acreditan polling terminado con Telegram lento o caído, outbox durable y pausa/reanudación.
- Se miden duración del polling y antigüedad de notificaciones pendientes.
- Semántica existente de polling intacta, diff total inferior a 60 KB y Maven/PostgreSQL ejecutados; revisión independiente limitada al diff completo.
