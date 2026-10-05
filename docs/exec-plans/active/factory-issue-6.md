# Issue #6: P3: Separar el despacho de Telegram del ciclo de polling

El alcance está definido y puede pasar a la siguiente fase local de escritura. El plan existente en `docs/exec-plans/active/factory-issue-6.md` fija una instancia, conserva las protecciones PostgreSQL y no requiere otra decisión funcional. En esta fase no hice cambios.

## Steps

- Desacoplar el envío del polling: dejar el polling encargado de persistir notificaciones en la outbox y procesarlas desde un dispatcher programado del monolito.
- Preservar el registro de entrega, la deduplicación y los claims actuales; acotar cada pasada por cantidad y duración, respetando el siguiente intento y el backoff existente para 429.
- Añadir mediciones de duración del polling y antigüedad de notificaciones pendientes mediante los mecanismos operativos existentes.
- Añadir pruebas con Telegram simulado lento y caído que cubran reintentos, pausa y reanudación, y comprueben que polling termina mientras los avisos permanecen en la outbox.
- Ejecutar Maven verify con Java 17 y revisar el resultado, incluidos los límites de evidencia si las pruebas PostgreSQL requieren Docker.

## Acceptance

- El dispatcher procesa la outbox independientemente del polling y limita cada pasada por mensajes y tiempo.
- Los avisos no elegibles esperan su siguiente intento; se conserva el backoff de 429 y la persistencia con deduplicación, sin afirmar exactly-once ni introducir claims locales duplicados.
- Las pruebas muestran que fallos o lentitud de Telegram no impiden terminar el polling y que las notificaciones pendientes permanecen en la outbox; también cubren pausa y reanudación.
- Quedan registradas la duración del polling y la antigüedad de las notificaciones pendientes.
